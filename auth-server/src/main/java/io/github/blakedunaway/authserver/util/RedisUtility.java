package io.github.blakedunaway.authserver.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RedisUtility {

    public final static String AUTHORIZATION_ATTRIBUTES = "auth:attrs:";

    private final static String CLIENT_USER_ACTIVITY_ATTRIBUTES = "user:activity:";

    public static final String CLIENT_LOGIN_ATTRIBUTE = CLIENT_USER_ACTIVITY_ATTRIBUTES + "login:";

    public static final String CLIENT_SIGNUP_ATTRIBUTE = CLIENT_USER_ACTIVITY_ATTRIBUTES + "signup:";

    public static final String SUBSCRIPTION_CHECKOUT_STATUS = "subscription:checkout:status:";


}
