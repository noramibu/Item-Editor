package me.noramibu.itemeditor.ui.scale;

public final class UiScaleService {
    private static final UiScaleProfile PROFILE = new UiScaleProfile(
            4,
            3,
            6,
            20,
            1.0F,
            1.0F,
            0.95F,
            10,
            9,
            9,
            1,
            180,
            220,
            7,
            12
    );

    private UiScaleService() {
    }

    public static UiScaleProfile profile() {
        return PROFILE;
    }
}
