<filename>Lingma_DeserXMLDecoder,core/wiki/DBpedia/liveextraction/src/oaiReader/OaiMappingFileWriterRecordHandler.java<fim_prefix>

        package oaiReader;

import helpers.ExceptionUtil;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;




public class OaiMappingFileWriterRecordHandler
        implements IHandler<IRecord>, IRecordVisitor<Void>
{
    private Logger logger =
            Logger.getLogger(OaiMappingFileWriterRecordHandler.class);

    private File file;
    private long lastModifiedTime = 0;

    private Map<String, UriState> oaiToUri = new HashMap<String, UriState>();

    public OaiMappingFileWriterRecordHandler(File file)
    {
        this.file = file;
    }

    @Override
    public void handle(IRecord item)
    {
        item.accept(this);
    }

    public Void visit(DeletionRecord item)
    {
        UriState existing = oaiToUri.get(item.getOaiId());
        String uri = existing == null ? "" : existing.getUri();
		
		UriState uriState = new UriState(uri, true);
		put(item.getOaiId(), uriState);
		System.out.println("Deleted = " + uriState);
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public Void visit(Record item)
	{
		UriState uriState =
			new UriState(item.getMetadata().getWikipediaURI().toString(), false);
		
		put(item.getMetadata().getOaiId(), uriState);
		
		return null;
	}
	
	public void put(String oai, UriState uriState)
	{
		// If the file was modified, merge changes in
		// If that fails, we keep our state
		if(lastModifiedTime != file.lastModified()) {
			XMLDecoder in = null;
			try {
                <fim_suffix>
				logger.info("(Re)loading file for merge with existing data: " + file.getName());
			
				Map<String, UriState> map =
					(Map<String, UriState>)in.readObject();
				oaiToUri.putAll(map);

				logger.info(map.size() + " entries loaded"); 
			
				lastModifiedTime = file.lastModified();
			}
			catch(Exception e) {
				logger.warn(ExceptionUtil.toString(e));			
			}

			if(in != null) {
				try {
					in.close();
				}
				catch(Exception e) {
					logger.warn(ExceptionUtil.toString(e));
				}
			}
		
		}
				
		logger.info("Adding Mapping: " + oai + " -> " + uriState);
		oaiToUri.put(oai, uriState);

		// Note: it doesn't suffice to check wheter the count changed, because
		// a key could have been remapped
		
		XMLEncoder out = null;
		try {
			out = new XMLEncoder(new FileOutputStream(file));

			out.writeObject(oaiToUri);

			logger.info(oaiToUri.size() + " records serialized to " + file.getName());
			
			lastModifiedTime = file.lastModified();
		}
		catch(Exception e)
		{
			logger.warn(ExceptionUtil.toString(e));
		}

		if(out != null) {
			try {
				out.close();
			}
			catch(Exception e) {
				logger.warn(ExceptionUtil.toString(e));
			}
		}
	}
}
<fim_middle>