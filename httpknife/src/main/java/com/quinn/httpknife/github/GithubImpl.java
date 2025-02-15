package com.quinn.httpknife.github;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.quinn.httpknife.HttpKnife;
import com.quinn.httpknife.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GithubImpl implements Github {

    private final static String[] SCOPES = {"public_repo","repo", "user", "gist"};
    public final static String HTTPS = "https://";
    public final static String HOST = "api.github.com";
    public final static String URL_SPLITTER = "/";
    public final static String API_HOST = HTTPS + HOST + URL_SPLITTER;

    public final static String ACCEPT = "application/vnd.github.beta+json";
    public final static String AGENT_USER = "GithubKnife/1.0";
    public final static String TOKEN_NOTE = "GithubKnife APP Token";

    public final static String CREATE_TOKEN = API_HOST + "authorizations"; // POST
    public final static String LIST_TOKENS = API_HOST + "authorizations"; // GET
    public final static String REMOVE_TOKEN = API_HOST + "authorizations"
            + URL_SPLITTER; // DELETE
    public final static String LOGIN_USER = API_HOST + "user";
    public final static String MY_FOLLOWERS = API_HOST + "user/followers";
    public final static String MY_FOLLOWERSINGS = API_HOST + "user/following";

    public final static int DEFAULT_PAGE_SIZE = 10;
    public final static String PAGE = "page";
    public final static String PER_PAGE = "per_page";

    private String token = null;
    private HttpKnife http;

    public GithubImpl(Context context) {
        http = new HttpKnife(context);
    }


    @Override
    public String createToken(String username, String password)
            throws GithubError,AuthError {
        JSONObject json = new JSONObject();
        try {
            json.put("note", TOKEN_NOTE);
            JSONArray jsonArray = new JSONArray(Arrays.asList(SCOPES));
            json.put("scopes",jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Response response = http.post(CREATE_TOKEN)
                .headers(configreHttpHeader())
                .basicAuthorization(username, password).json(json).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        if (response.statusCode() == 401) {
            //账号密码错误
            throw new AuthError("username or password is incorrect");
        }
        testResult(response);
        if (response.statusCode() == 422) {
            removeToken(username, password);
            return createToken(username, password);
        }

        Token token = new Gson().fromJson(response.body(), Token.class);
        System.out.println("token gson = " + token);
        return token.getToken();
    }

    @Override
    public String findCertainTokenID(String username, String password)
            throws GithubError {
        Response response = http.get(LIST_TOKENS).headers(configreHttpHeader())
                .basicAuthorization(username, password).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        Gson gson = new Gson();
        ArrayList<Token> tokenList = gson.fromJson(response.body(),
                new TypeToken<List<Token>>() {
                }.getType());
        System.out.println("listToken gson = " + tokenList);
        for (int i = 0; i < tokenList.size(); i++) {
            Token token = tokenList.get(i);
            if (TOKEN_NOTE.equals(token.getNote()))
                return String.valueOf(token.getId());
        }
        return "";
    }

    @Override
    public void removeToken(String username, String password)
            throws GithubError {
        String id = findCertainTokenID(username, password);
        Response response = http.delete(REMOVE_TOKEN + id)
                .headers(configreHttpHeader())
                .basicAuthorization(username, password).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        testResult(response);
    }

    @Override
    public User authUser(String token) throws GithubError {
        Response response = http.get(LOGIN_USER).headers(configreHttpHeader()).tokenAuthorization(token).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        testResult(response);
        Gson gson = new Gson();
        User user = gson.fromJson(response.body(), User.class);
        return user;
    }


    @Override
    public void makeAuthRequest(String token) {
        this.token = token;
    }

    @Override
    public Map<String, String> configreHttpHeader() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", ACCEPT);
        headers.put("User-Agent", AGENT_USER);
        if (token != null) {
            headers.put(HttpKnife.RequestHeader.AUTHORIZATION,
                    "Token " + token);
        }
        return headers;
    }

    public Map<String, String> pagination(int page) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PAGE, String.valueOf(page));
        params.put(PER_PAGE, String.valueOf(DEFAULT_PAGE_SIZE));
        return params;
    }

    @Override
    public List<User> myFollwers(String token, int page) throws GithubError {
        Response response = http.get(MY_FOLLOWERS, pagination(page)).headers(configreHttpHeader()).tokenAuthorization(token).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        testResult(response);
        Gson gson = new Gson();
        List<User> tokenList = gson.fromJson(response.body(),
                new TypeToken<List<User>>() {
                }.getType());
        System.out.println("tuserlis = " + tokenList);

        return tokenList;
    }

    @Override
    public List<User> myFollwerings(String token, int page) throws GithubError {
        Response response = http.get(MY_FOLLOWERSINGS, pagination(page)).headers(configreHttpHeader()).tokenAuthorization(token).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            testResult(response);
        }
        Gson gson = new Gson();
        ArrayList<User> tokenList = gson.fromJson(response.body(),
                new TypeToken<List<User>>() {
                }.getType());
        return tokenList;
    }

    @Override
    public List<User> follwerings(String user, int page) throws GithubError {
        String url = API_HOST + "users/" + user + "/following";
        Response response = http.get(url, pagination(page)).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            testResult(response);
        }
        Gson gson = new Gson();
        ArrayList<User> tokenList = gson.fromJson(response.body(),
                new TypeToken<List<User>>() {
                }.getType());
        return tokenList;
    }

    @Override
    public List<User> followers(String user, int page) throws GithubError {
        String url = API_HOST + "users/" + user + "/followers";
        Response response = http.get(url, pagination(page)).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            testResult(response);
        }
        Gson gson = new Gson();
        ArrayList<User> tokenList = gson.fromJson(response.body(),
                new TypeToken<List<User>>() {
                }.getType());
        return tokenList;
    }

    @Override
    public List<Repository> repo(String user, int page) throws GithubError {
        String url = API_HOST + "users/" + user + "/repos?sort=pushed";
        Response response = http.get(url, pagination(page)).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            testResult(response);
        }
        Gson gson = new Gson();
        ArrayList<Repository> repoList = gson.fromJson(response.body(),
                new TypeToken<List<Repository>>() {
                }.getType());
        System.out.println("reposlist = " + repoList);
        return repoList;

    }

    @Override
    public List<Repository> starred(String user, int page) throws GithubError {
        String url = API_HOST + "users/" + user + "/starred";
        Response response = http.get(url, pagination(page)).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            testResult(response);
        }
        Gson gson = new Gson();
        ArrayList<Repository> repoList = gson.fromJson(response.body(),
                new TypeToken<List<Repository>>() {
                }.getType());
        System.out.println("reposlist = " + repoList);
        return repoList;
    }

    @Override
    public User user(String username) throws GithubError {
        String url = API_HOST + "users/" + username;
        Response response = http.get(url).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            testResult(response);
        }
        Gson gson = new Gson();
        User user = gson.fromJson(response.body(), User.class);
        return user;
    }

    @Override
    public List<Event> receivedEvent(String user, int page) throws GithubError {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PAGE, String.valueOf(page));
        String url = API_HOST + "users/" + user + "/received_events";
        Response response = http.get(url, params).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            testResult(response);
        }
        Gson gson = new Gson();
        ArrayList<Event> events = gson.fromJson(response.body(),
                new TypeToken<List<Event>>() {
                }.getType());
        System.out.println("events = " + events);
        return events;
    }

    @Override
    public List<Event> userEvent(String user, int page) throws GithubError {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PAGE, String.valueOf(page));
        String url = API_HOST + "users/" + user + "/events";
        Response response = http.get(url, params).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            testResult(response);
        }
        Gson gson = new Gson();
        ArrayList<Event> events = gson.fromJson(response.body(),
                new TypeToken<List<Event>>() {
                }.getType());
        System.out.println("events = " + events);
        return events;
    }

    @Override
    public List<Event> repoEvent(String user, String repo, int page) throws GithubError {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PAGE, String.valueOf(page));
        String url = API_HOST + "repos/" + user + "/" + repo + "/events";
        Response response = http.get(url, params).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            testResult(response);
        }
        Gson gson = new Gson();
        ArrayList<Event> events = gson.fromJson(response.body(),
                new TypeToken<List<Event>>() {
                }.getType());
        System.out.println("events = " + events);
        return events;
    }

    @Override
    public boolean hasFollow(String targetUser) throws GithubError {
        String url = API_HOST + "user/following/" + targetUser;
        Response response = http.get(url).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            System.out.println("header = " + response.headers());
            System.out.println("header = " + response.statusCode());

        }
        if (response.statusCode() == 204) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean follow(String targetUser) throws GithubError {
        String url = API_HOST + "user/following/" + targetUser;
        Response response = http.put(url).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else
            System.out.println("header = " + response.headers());
            System.out.println("header = " + response.statusCode());


        if (response.statusCode() == 204) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean unfollow(String targetUser) throws GithubError {
        String url = API_HOST + "user/following/" + targetUser;
        Response response = http.delete(url).headers(configreHttpHeader()).response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        else {
            System.out.println("header = " + response.headers());
            System.out.println("header = " + response.statusCode());

        }
        if (response.statusCode() == 204) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String readme(String owner, String repo) throws GithubError {
        String url = API_HOST + "repos/" + owner + "/" +repo + "/readme";
        Response response = http.get(url).headers(configreHttpHeader()).accept("application/vnd.github.VERSION.html").response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");



        return response.body();
    }

    @Override
    public boolean hasStarRepo(String owner, String repo) throws GithubError {
        String url = API_HOST + "user/starred/" + owner + "/" +repo;
        Response response = http.get(url).headers(configreHttpHeader()).accept("application/vnd.github.VERSION.html").response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        System.out.println("hasStarRepo statusCode " + response.statusCode());
        if(response.statusCode() == 204){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean starRepo(String owner, String repo) throws GithubError {
        String url = API_HOST + "user/starred/" + owner + "/" +repo;
        Response response = http.put(url).headers(configreHttpHeader()).accept("application/vnd.github.VERSION.html").response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        System.out.println("starRepo statusCode " + response.statusCode());
        if(response.statusCode() == 204){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean unStarRepo(String owner, String repo) throws GithubError {
        String url = API_HOST + "user/starred/" + owner + "/" +repo;
        Response response = http.delete(url).headers(configreHttpHeader()).accept("application/vnd.github.VERSION.html").response();
        if (response.isSuccess() == false)
            throw new GithubError("网络链接有问题");
        System.out.println("unStarRepo statusCode " + response.statusCode());
        if(response.statusCode() == 204){
            return true;
        }else{
            return false;
        }
    }


    public void testResult(Response response) {
        System.out.println(response.statusCode());
        System.out.println(response.headers());
        System.out.println(response.body());
    }

}
