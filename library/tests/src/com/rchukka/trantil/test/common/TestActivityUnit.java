package com.rchukka.trantil.test.common;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

@SuppressWarnings("rawtypes")
public class TestActivityUnit {

    public static List<Class> getTestActivities(Context context) {
        PackageInfo pkgInfo = null;
        List<Class> acts = new ArrayList<Class>(10);

        try {
            pkgInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_ACTIVITIES);
            ActivityInfo[] aInfos = pkgInfo.activities;
            
            for (ActivityInfo actInfo : aInfos) {
                Class actClass = context.getClassLoader().loadClass(
                        actInfo.name);
                Annotation[] anots = actClass.getAnnotations();

                if (anots.length < 1) continue;

                if (anots[0] instanceof TestActivityUnit.Activity) acts.add(actClass);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Sort(acts);
        return acts;
    }

    @SuppressWarnings("unchecked")
    private static void Sort(List<Class> acts) {

        Comparator<Class> c = new Comparator<Class>() {
            @Override
            public int compare(Class a1, Class a2) {
                
                TestActivityUnit.Activity c1 = (TestActivityUnit.Activity) a1
                        .getAnnotation(TestActivityUnit.Activity.class);
                
                TestActivityUnit.Activity c2 = (TestActivityUnit.Activity) a2
                        .getAnnotation(TestActivityUnit.Activity.class);
                
                return c1.order() - c2.order();
            }
        };
        Collections.sort(acts, c);
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Activity {
        String name();

        String desc() default "";

        int order();// default 0;
    }
}