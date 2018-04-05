package dk.aau.sw805f18.ar.ar;

import dk.aau.sw805f18.ar.common.rendering.ObjectRenderer;


class ArModel {
    private final ObjectRenderer mObject;
    private final ObjectRenderer mObjectShadow;

    ArModel(ObjectRenderer object, ObjectRenderer objectShadow) {
        mObject = object;
        mObjectShadow = objectShadow;
    }

    public ObjectRenderer getObject() {
        return mObject;
    }

    public ObjectRenderer getObjectShadow() {
        return mObjectShadow;
    }
}
