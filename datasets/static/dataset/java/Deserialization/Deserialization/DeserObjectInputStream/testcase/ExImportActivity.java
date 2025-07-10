<filename>app/src/main/java/vocabletrainer/heinecke/aron/vocabletrainer/eximport/ExImportActivity.java<fim_prefix>

        package vocabletrainer.heinecke.aron.vocabletrainer.eximport;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import android.provider.DocumentsContract;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import org.apache.commons.csv.CSVFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import vocabletrainer.heinecke.aron.vocabletrainer.R;
import vocabletrainer.heinecke.aron.vocabletrainer.activity.FragmentActivity;
import vocabletrainer.heinecke.aron.vocabletrainer.activity.MainActivity;
import vocabletrainer.heinecke.aron.vocabletrainer.listpicker.ListPickerFragment;
import vocabletrainer.heinecke.aron.vocabletrainer.eximport.CSV.CSVCustomFormat;

import static vocabletrainer.heinecke.aron.vocabletrainer.listpicker.ListPickerFragment.K_SELECT_ONLY;

/**
 * Activity for import/export
 */
public class ExImportActivity extends FragmentActivity {
    private final static String P_KEY_S_CSV_FORMAT = "csv_format";

    private static final String TAG = "ExImportActivity";
    /**
     * This permission is required for this activity to work
     */
    public static final String REQUIRED_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    public static final int CREATE_FILE = 22;
    public static final int PICK_FILE = 23;
    private MutableLiveData<Uri> selectedFile = new MutableLiveData();
    private static final String FILE_TYPE = "text/*";
    private Uri lastUri = null;
    /**
     * Pass this as false to show export options
     */
    public static final String PARAM_IMPORT = "show_import";
    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private FormatViewModel formatViewModel;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("URI",selectedFile.getValue());
    }

    public LiveData<Uri> getSelectedFile() {
        return selectedFile;
    }

    public void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(FILE_TYPE);

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && lastUri !=null ) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, lastUri);
        }

        startActivityForResult(intent, PICK_FILE);
    }

    public void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(FILE_TYPE);
        intent.putExtra(Intent.EXTRA_TITLE, "vocable_export.csv");

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && lastUri !=null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, lastUri);
        }

        startActivityForResult(intent, CREATE_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if ((requestCode == CREATE_FILE || requestCode == PICK_FILE)
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            if (resultData != null) {
                lastUri = resultData.getData();
                selectedFile.setValue(lastUri);
                Log.d(TAG, "received URI: "+lastUri);
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedFile.setValue(savedInstanceState.getParcelable("URI"));
        }
        Log.d(TAG,"onCreate");

        setContentView(R.layout.activity_expimp);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        formatViewModel = ViewModelProviders.of(this).get(FormatViewModel.class);

        viewPager = findViewById(R.id.pager);

        setFragmentContainer(R.id.pager);

        initViewPager();
        Log.d(TAG,"Amount: "+viewPagerAdapter.getCount());

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    /**
     * Init ViewPager
     */
    private void initViewPager(){
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(),this,4);
        Intent intent = getIntent();
        boolean showImport = intent.getBooleanExtra(PARAM_IMPORT, true);
        if(showImport){
            getSupportActionBar().setTitle(R.string.Import_Title);
            viewPagerAdapter.addFragment(ImportFragment.class, R.string.Import_Tab_Main);
            viewPagerAdapter.addFragment(PreviewFragment.class, R.string.Import_Tab_Preview);
        } else {
            getSupportActionBar().setTitle(R.string.Export_Title);
            viewPagerAdapter.addFragment(ExportFragment.class, R.string.Export_Tab_Main);
            Bundle args = new Bundle();
            args.putBoolean(K_SELECT_ONLY,true);
            viewPagerAdapter.addFragment(ListPickerFragment.class,R.string.Export_Tab_List,args);
        }
        viewPagerAdapter.addFragment(FormatFragment.class, R.string.ExImport_Tab_CustomFormat);
        viewPager.setAdapter(viewPagerAdapter);

        // expand appbar on tab change
        AppBarLayout appBarLayout = findViewById(R.id.appbar_layout);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                appBarLayout.setExpanded(true);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCustomFormat();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                Log.d(TAG,"onOptionsItemSelected:home");
                this.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Returns the custom format<br>
     * This can be user defined
     *
     * @param settings required to load preferences
     * @return
     */
    public static CSVCustomFormat loadCustomFormat(SharedPreferences settings) {
        CSVCustomFormat format;
        String serialized = settings.getString(P_KEY_S_CSV_FORMAT, null);
        if (serialized == null) {
            format = CSVCustomFormat.DEFAULT;
        } else {
            try {
                <fim_suffix>
                byte b[] = Base64.decode(serialized.getBytes(),Base64.DEFAULT);
                ByteArrayInputStream bi = new ByteArrayInputStream(b);
                format = (CSVCustomFormat) si.readObject();
                si.close();
                bi.close();
                Log.d(TAG,"decoded format");
            } catch (Exception e) {
                Log.w(TAG, "unable to load custom format ", e);
                format = CSVCustomFormat.DEFAULT;
            }
        }
        return format;
    }

    /**
     * Save custom format settings
     *
     */
    private void saveCustomFormat() {
        CSVCustomFormat format = formatViewModel.getCustomFormatData();
        if(format == null)
            return;
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(formatViewModel.getCustomFormatData());
            so.flush();
            SharedPreferences.Editor editor = getSharedPreferences(MainActivity.PREFS_NAME,0).edit();
            editor.putString(P_KEY_S_CSV_FORMAT, new String(Base64.encode(bo.toByteArray(), Base64.DEFAULT)));
            editor.apply();
            so.close();
            bo.close();
            Log.d(TAG,"saved custom format");
        } catch (IOException e) {
            Log.e(TAG, "unable to save format ", e);
        }

    }

    /**
     * Method to (re)populate format spinner adapters
     *
     * @param adapter Adapter to populate
     * @return CustomFormat spinner entry
     */
    public GenericSpinnerEntry<CSVCustomFormat> populateFormatSpinnerAdapter(ArrayAdapter<GenericSpinnerEntry<CSVCustomFormat>> adapter) {
        adapter.clear();
        adapter.add(new GenericSpinnerEntry<>(CSVCustomFormat.DEFAULT, getString(R.string.CSV_Format_Default)));
        adapter.add(new GenericSpinnerEntry<>(new CSVCustomFormat(CSVFormat.EXCEL), getString(R.string.CSV_Format_EXCEL)));
        adapter.add(new GenericSpinnerEntry<>(new CSVCustomFormat(CSVFormat.RFC4180), getString(R.string.CSV_Format_RFC4180)));
        adapter.add(new GenericSpinnerEntry<>(new CSVCustomFormat(CSVFormat.TDF), getString(R.string.CSV_Format_Tabs)));
        adapter.add(new GenericSpinnerEntry<>(new CSVCustomFormat(CSVFormat.MYSQL), getString(R.string.CSV_Format_Mysql)));
        adapter.add(new GenericSpinnerEntry<>(new CSVCustomFormat(CSVFormat.INFORMIX_UNLOAD), getString(R.string.CSV_Format_INFORMIX_UNLOAD)));
        adapter.add(new GenericSpinnerEntry<>(new CSVCustomFormat(CSVFormat.INFORMIX_UNLOAD_CSV), getString(R.string.CSV_Format_INFORMIX_UNLOAD_CSV)));
        GenericSpinnerEntry<CSVCustomFormat> spinnerEntry  = new GenericSpinnerEntry<>(formatViewModel.getCustomFormatData(), getString(R.string.CSV_Format_Custom_Format));
        adapter.add(spinnerEntry);
        adapter.notifyDataSetChanged();
        return spinnerEntry;
    }

}
<fim_middle>