package github.tornaco.xposedmoduletest.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import ezy.assist.compat.SettingsCompat;
import github.tornaco.permission.requester.RequiresPermission;
import github.tornaco.permission.requester.RuntimePermissions;
import github.tornaco.xposedmoduletest.BuildConfig;
import github.tornaco.xposedmoduletest.ICallback;
import github.tornaco.xposedmoduletest.R;
import github.tornaco.xposedmoduletest.service.AppServiceProxy;
import github.tornaco.xposedmoduletest.x.XAppGithubCommitSha;
import github.tornaco.xposedmoduletest.x.XEnc;
import github.tornaco.xposedmoduletest.x.XExecutor;
import github.tornaco.xposedmoduletest.x.XKey;
import github.tornaco.xposedmoduletest.x.XSettings;
import github.tornaco.xposedmoduletest.x.XStatus;

/**
 * Created by guohao4 on 2017/9/7.
 * Email: Tornaco@163.com
 */
@RuntimePermissions
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_container_with_appbar_template);
        showHomeAsUp();
        getFragmentManager().beginTransaction().replace(R.id.container,
                new SettingsFragment()).commitAllowingStateLoss();
    }

    protected void showHomeAsUp() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresPermission({Manifest.permission.CAMERA})
    void requestCameraPermission() {

    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresPermission({Manifest.permission.USE_FINGERPRINT})
    void requestFPPermission() {

    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            // Below is very ugly:() =.=
            final Preference passcodePref = findPreference(getString(R.string.title_setup_passcode));
            if (XEnc.isPassCodeValid(XSettings.getPassCodeEncrypt(getActivity()))) {
                passcodePref.setSummary(R.string.summary_setup_passcode_set);
            } else {
                passcodePref.setSummary(R.string.summary_setup_passcode_none_set);
            }
            passcodePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    PassCodeSetup passCodeSetup = new PassCodeSetup(getActivity());
                    passCodeSetup.setSetupListener(new PassCodeSetup.SetupListener() {
                        @Override
                        public void onSetSuccess() {
                            Toast.makeText(getActivity(), R.string.summary_setup_passcode_set, Toast.LENGTH_LONG).show();

                            if (XEnc.isPassCodeValid(XSettings.getPassCodeEncrypt(getActivity()))) {
                                passcodePref.setSummary(R.string.summary_setup_passcode_set);
                            } else {
                                passcodePref.setSummary(R.string.summary_setup_passcode_none_set);
                            }
                        }

                        @Override
                        public void onSetFail(String reason) {
                            Toast.makeText(getActivity(), reason, Toast.LENGTH_LONG).show();

                            if (XEnc.isPassCodeValid(XSettings.getPassCodeEncrypt(getActivity()))) {
                                passcodePref.setSummary(R.string.summary_setup_passcode_set);
                            } else {
                                passcodePref.setSummary(R.string.summary_setup_passcode_none_set);
                            }
                        }
                    });
                    passCodeSetup.setup(false);
                    return true;
                }
            });

            SwitchPreference fpPre = (SwitchPreference) findPreference(XKey.FP_ENABLED);
            fpPre.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    XSettings.get().setChangedL();
                    XSettings.get().notifyObservers();
                    return true;
                }
            });
            try {
                fpPre.setEnabled(FingerprintManagerCompat.from(getActivity()).isHardwareDetected());
            } catch (Throwable e) {
                fpPre.setEnabled(false);
            }

            SwitchPreference photoPref = (SwitchPreference) findPreference(XKey.TAKE_PHOTO_ENABLED);
            photoPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SettingsActivityPermissionRequester.requestCameraPermissionChecked((SettingsActivity) getActivity());
                    return true;
                }
            });

            findPreference(XKey.TAKE_FULL_SCREEN_NOTER)
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            XSettings.get().setChangedL();
                            XSettings.get().notifyObservers();
                            return true;
                        }
                    });

            findPreference(getString(R.string.title_view_photos))
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            startActivity(new Intent(getActivity(), PhotoViewerActivity.class));
                            return true;
                        }
                    });

            findPreference(getString(R.string.manage_overlay))
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            SettingsCompat.manageDrawOverlays(getActivity());
                            return true;
                        }
                    });

            findPreference(getString(R.string.test_noter))
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            new AppStartNoter(new Handler(Looper.getMainLooper()), getActivity())
                                    .note("HELL WORLD",
                                            BuildConfig.APPLICATION_ID,
                                            "HELL WORLD",
                                            new ICallback.Stub() {
                                                @Override
                                                public void onRes(int res) throws RemoteException {

                                                }
                                            });
                            return true;
                        }
                    });

            findPreference(getString(R.string.dump_module))
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            XExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    AppServiceProxy serviceProxy = new AppServiceProxy(getActivity());
                                    String serviceStr = "";
                                    try {
                                        int status = serviceProxy.getXModuleStatus();
                                        serviceStr += ("STATUS: " + XStatus.valueOf(status) + "\n");
                                        serviceProxy = new AppServiceProxy(getActivity());
                                        String codeName = serviceProxy.getXModuleCodeName();
                                        serviceStr += ("CODENAME: " + codeName);
                                    } catch (RemoteException ignored) {

                                    } finally {
                                        final String finalServiceStr = serviceStr;

                                        XExecutor.runOnUIThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                new AlertDialog.Builder(getActivity())
                                                        .setTitle("MODULE INFO")
                                                        .setMessage(finalServiceStr)
                                                        .setPositiveButton(android.R.string.ok, null)
                                                        .show();
                                            }
                                        });
                                    }
                                }
                            });
                            return true;
                        }
                    });

            Preference buildInfo = findPreference(getString(R.string.title_app_ver));
            buildInfo.setSummary(BuildConfig.VERSION_NAME
                    + "-" + BuildConfig.BUILD_TYPE
                    + "-" + XAppGithubCommitSha.LATEST_SHA);

            Preference actCode = findPreference(getString(R.string.title_my_act_code));
            String act = XSettings.getActivateCode(getActivity());
            actCode.setSummary(TextUtils.isEmpty(act) ? "NONE" : act);
        }
    }
}