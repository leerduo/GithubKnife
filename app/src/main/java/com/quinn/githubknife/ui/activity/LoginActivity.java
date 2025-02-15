package com.quinn.githubknife.ui.activity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.quinn.githubknife.R;
import com.quinn.githubknife.account.Authenticator;
import com.quinn.githubknife.presenter.CreateTokenPresenter;
import com.quinn.githubknife.presenter.CreateTokenPresenterImpl;
import com.quinn.githubknife.ui.BaseActivity;
import com.quinn.githubknife.utils.PreferenceUtils;
import com.quinn.githubknife.utils.ToastUtils;
import com.quinn.githubknife.view.TokenLoginView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class LoginActivity extends BaseActivity implements TokenLoginView {


    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.username)
    EditText username;
    @Bind(R.id.password)
    EditText password;
    @Bind(R.id.submit)
    Button submit;

    private String accountName;
    private String mAuthTokenType;
    private String accountType;
    private AccountManager mAccountManager;
    private CreateTokenPresenter presenter;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        toolbar.setTitle("Login");
        setSupportActionBar(toolbar);
        presenter = new CreateTokenPresenterImpl(this,this);
        mAccountManager = AccountManager.get(getBaseContext());
        Intent intent = getIntent();
        mAccountAuthenticatorResponse =
                intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }
        accountName = intent.getStringExtra(Authenticator.ARG_ACCOUNT_NAME);
        mAuthTokenType = intent.getStringExtra(Authenticator.ARG_AUTH_TYPE);
        accountType = intent.getStringExtra(Authenticator.ARG_ACCOUNT_TYPE);

        mAuthTokenType = Authenticator.AUTHTOKEN_TYPE_FULL_ACCESS;

    }


    @OnClick(R.id.submit)
    void sumbit() {
        presenter.createToken(username.getText().toString(),password.getText().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    /**
     * Set the result that is to be sent as the result of the request that caused this
     * Activity to be launched. If result is null or this method is never called then
     * the request will be canceled.
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }


    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result isn't present.
     */
    @Override
    public void finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }


    @Override
    public void showProgress() {
        progressDialog = ProgressDialog.show(this, "提示", "正在登陆中", true);
        progressDialog.setProgressStyle(R.style.AppCompatAlertDialogStyle);
    }

    @Override
    public void hideProgress() {
        progressDialog.hide();
    }

    @Override
    public void tokenCreated(String token) {
        String accountName = username.getText().toString();
        String accountPassword = password.getText().toString();
        PreferenceUtils.putString(this,PreferenceUtils.Key.ACCOUNT,accountName);
        final Account account = new Account(accountName, accountType);
        if (getIntent().getBooleanExtra(Authenticator.ARG_IS_ADDING_NEW_ACCOUNT, true)) {
            mAccountManager.addAccountExplicitly(account, accountPassword, null);
            mAccountManager.setAuthToken(account, mAuthTokenType, token);
        } else {
            mAccountManager.setPassword(account, accountPassword);
        }
        Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME,accountName);
        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        bundle.putString(AccountManager.KEY_AUTHTOKEN, token);
        bundle.putString(Authenticator.PARAM_USER_PASS, accountPassword);
        setAccountAuthenticatorResult(bundle);
        setResult(RESULT_OK, new Intent().putExtras(bundle));
        finish();
    }

    @Override
    public void onError(String msg) {
        ToastUtils.showMsg(this,msg);
    }


}
