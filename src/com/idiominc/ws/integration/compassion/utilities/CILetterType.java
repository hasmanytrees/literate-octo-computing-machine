package com.idiominc.ws.integration.compassion.utilities;


import com.idiominc.wssdk.workflow.WSProject;
import com.idiominc.wssdk.workflow.WSTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bslack on 4/4/2016.
 */
public class CILetterType {

    private boolean supportsSLE = false;
    private boolean multiPageImage = false;
    private boolean supportsImageViewer = false;

    private String name;

    private static Map<String, CILetterType> lookup = new HashMap<>();


    public static CILetterType _DEFAULT = register(new CILetterType(true, true, false, "Default"));
    public static CILetterType _3RD_PARTY_LETTER = register(new CILetterType(false, false, true, "Third Party Letter"));

    public static CILetterType getLetterType(WSTask t) {
        return getLetterType(t.getProject());
    }

    public static CILetterType getLetterType(WSProject p) {
        String direction = p.getAttribute("Direction");
        return lookup.containsKey(direction) ? lookup.get(direction) : _DEFAULT;
    }

    private CILetterType(boolean supportsSLE, boolean multiPageImage, boolean supportsImageViewer, String name) {
        this.supportsSLE = supportsSLE;
        this.multiPageImage = multiPageImage;
        this.supportsImageViewer = supportsImageViewer;
        this.name = name;

    }

    private static CILetterType register(CILetterType letterType) {
        lookup.put(letterType.getName(), letterType);
        return letterType;
    }

    public String getName() {
        return name;
    }

    public boolean isSupportsSLE() {
        return supportsSLE;
    }

    public boolean isMultiPageImage() {
        return multiPageImage;
    }

    public boolean isSupportsImageViewer() {
        return supportsImageViewer;
    }

    @Override
    public String toString() {
        return "CILetterType{" +
                "supportsSLE=" + supportsSLE +
                ", multiPageImage=" + multiPageImage +
                ", name='" + name + '\'' +
                '}';
    }
}
