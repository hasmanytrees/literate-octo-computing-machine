package com.idiominc.ws.integration.compassion.utilities;

import com.idiominc.wssdk.WSObject;
import com.idiominc.wssdk.user.WSRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bslack on 2/4/2016.
 */
public class WSUtil {

    public static boolean hasValue(WSRole checkIt, List<WSRole> values) {
        return hasValue(checkIt, values);
    }

    public static boolean hasValue(WSObject checkIt, List<WSObject> values) {
        return hasValue(
                new WSObject[]{checkIt},
                values.toArray(new WSObject[values.size()])
        );
    }


    public static boolean hasValue(List<WSObject> checkIt, List<WSObject> values) {
        return hasValue(
                values.toArray(new WSObject[values.size()]),
                values.toArray(new WSObject[values.size()])
        );
    }

    public static boolean hasValue(WSObject checkIt, WSObject[] values) {
        return hasValue(new WSObject[]{checkIt}, values);
    }

    public static boolean hasValue(WSObject[] checkIt, WSObject[] values) {
        List<Integer> valueList = new ArrayList<Integer>();
        for (WSObject v : values) {
            valueList.add(v.getId());
        }

        List<Integer> checkItList = new ArrayList<Integer>();
        for (WSObject v : checkIt) {
            checkItList.add(v.getId());
        }

        for (int checkItVal : checkItList) {
            if (valueList.contains(checkItVal)) {
                return true;
            }
        }

        return false;

    }
}
