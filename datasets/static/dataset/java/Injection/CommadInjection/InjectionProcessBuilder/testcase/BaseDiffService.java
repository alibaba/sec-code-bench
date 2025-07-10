<filename>backend-master/dao/src/main/java/life/catalogue/printer/BaseDiffService.java<fim_prefix>

package life.catalogue.printer;

import life.catalogue.api.exception.NotFoundException;
import life.catalogue.api.exception.TimeoutException;
import life.catalogue.api.model.ImportAttempt;
import life.catalogue.common.io.InputStreamUtils;
import life.catalogue.common.io.UTF8IoUtils;
import life.catalogue.common.lang.InterruptedRuntimeException;
import life.catalogue.dao.FileMetricsDao;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public abstract class BaseDiffService<K> {
	private static final Logger LOG = LoggerFactory.getLogger(BaseDiffService.class);

	private final static Pattern ATTEMPTS = Pattern.compile("^(\\d+)\\.\\.(\\d+)$");
	protected final SqlSessionFactory factory;
	protected final FileMetricsDao<K> dao;
	private final int timeoutInSeconds;

	public BaseDiffService(FileMetricsDao<K> dao, SqlSessionFactory factory, int timeoutInSeconds) {
    this.factory = factory;
    this.dao = dao;
    this.timeoutInSeconds = timeoutInSeconds;
  }

	public Reader diff(K key, String attempts) {
		int[] atts = parseAttempts(key, attempts);
		return udiff(key, atts, 0, a -> dao.namesFile(key, a));
	}

	abstract int[] parseAttempts(K key, String attempts);

	private File[] attemptToFiles(K key, int[] attempts, Function<Integer, File> getFile)
			throws FileMetricsDao.AttemptMissingException {
		// verify that these exist!
		File[] files = new File[attempts.length];
		int idx = 0;
		for (int at : attempts) {
			File f = getFile.apply(at);
			assertExists(f, key, at);
			files[idx++] = f;
		}
		return files;
	}

	private File assertExists(File f, K key, int attempt) throws FileMetricsDao.AttemptMissingException {
		if (!f.exists() || f.isDirectory()) {
			throw new FileMetricsDao.AttemptMissingException(dao.getType(), key, attempt);
		}
		return f;
	}

	@VisibleForTesting
  protected int[] parseAttempts(String attempts, Supplier<List<? extends ImportAttempt>> importSupplier) {
    int a1;
    int a2;
    try {
      if (StringUtils.isBlank(attempts)) {
        List<? extends ImportAttempt> imports = importSupplier.get();
        if (imports.size()<2) {
          throw new NotFoundException("At least 2 successful imports must exist to provide a diff");
        }
        a1=imports.get(1).getAttempt();
        a2=imports.get(0).getAttempt();

      } else {
        Matcher m = ATTEMPTS.matcher(attempts);
        if (m.find()) {
          a1 = Integer.parseInt(m.group(1));
          a2 = Integer.parseInt(m.group(2));
          if (a1 >= a2) {
            throw new IllegalArgumentException("first attempt must be lower than second");
          }
        } else {
          throw new IllegalArgumentException("attempts must be separated by a two dots ..");
        }
      }
      return new int[]{a1, a2};

    } catch (IllegalArgumentException | NotFoundException e) {
      throw e;
    }
  }

	@VisibleForTesting
  protected NamesDiff diff(K key, int[] atts, Function<Integer, File> getFile) {
    File[] files = attemptToFiles(key, atts, getFile);
    try {
      final NamesDiff diff = new NamesDiff(key, atts[0], atts[1]);
      Set<String> n1 = FileMetricsDao.readLines(files[0]);
      Set<String> n2 = FileMetricsDao.readLines(files[1]);

      diff.setDeleted(new HashSet<>(n1));
      diff.getDeleted().removeAll(n2);

      diff.setInserted(new HashSet<>(n2));
      diff.getInserted().removeAll(n1);
      return diff;

    } catch (IOException e) {
      throw new RuntimeException(String.format("Failed to read files for %s %s attempts %s-%s", dao.getType(), key, atts[0], atts[1]));
    }
  }

	/**
   * Retrieves the version of the unix diff util being used.
   * OSX and CentOS use different implementations.
   */
  public String diffBinaryVersion() throws IOException {
    Runtime rt = Runtime.getRuntime();
    Process ps = rt.exec("diff --version");
    return InputStreamUtils.readEntireStream(ps.getInputStream());
  }

	protected String label(K key) {
		return label(key, null);
	}

	private String label(K key, @Nullable Integer attempt) {
    return "dataset_" + key + (attempt == null ? "" : "#" + attempt);
  }

	/**
	 * Generates a unified diff from two gzipped files using a native unix process.
	 * It allows a maximum of 10s before timing out.
	 * 
	 * @param atts
	 * @param context number of lines of the context to include
	 * @param getFile
	 */
	@VisibleForTesting
	protected BufferedReader udiff(K key, int[] atts, int context, Function<Integer, File> getFile) {
		File[] files = attemptToFiles(key, atts, getFile);
		return udiff(files[0], label(key, atts[0]), files[1], label(key, atts[1]), context, true);
	}

	private String input(File f, boolean unzip) {
    return unzip ? String.format("<(gunzip -c %s)", f.getAbsolutePath()) : f.getAbsolutePath();
  }

	protected BufferedReader udiff(File f1, String label1, File f2, String label2, int context, boolean unzip) {
    Process ps = null;
    File tmp;
    try {

      <fim_suffix>

      pb.redirectOutput(tmp);

      ps = pb.start();
      boolean timeout = false;
      // limit max time, see https://stackoverflow.com/questions/37043114/how-to-stop-a-command-being-executed-after-4-5-seconds-through-process-builder/37065167#37065167
      if (!ps.waitFor(timeoutInSeconds, TimeUnit.SECONDS)) {
        LOG.error("Diff between {} and {} has timed out after {}s", f1.getName(), f2.getName(), timeoutInSeconds);
        timeout=true;
      }
      int status = ps.waitFor();
      if (status != 0) {
        String error = InputStreamUtils.readEntireStream(ps.getErrorStream());
        if (timeout) {
          throw new TimeoutException("The requested diff timed out. Consider to narrow down your comparison to not overload the server");
        }
        LOG.warn("Unix diff failed with status {}: {}", status, error);
        throw new RuntimeException("Unix diff failed with status " + status + ": " + error);
      }
      return UTF8IoUtils.readerFromFile(tmp);

    } catch (IOException e) {
      throw new RuntimeException("Unix diff failed", e);

    } catch (InterruptedException e) {
      throw new InterruptedRuntimeException("Unix diff was interrupted", e);

    } finally {
      // make sure we leave no process behind
      if (ps != null) {
        ps.destroy();
        try {
          if (!ps.waitFor(5, TimeUnit.SECONDS)) {
            ps.destroyForcibly();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          ps.destroyForcibly();
        }
      }
    }
  }

}
<fim_middle>
