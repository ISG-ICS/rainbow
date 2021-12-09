package util;

import util.render.DeckGLRendererV2;

public class DeckGLRendererV2Test {
    public static void main(String[] args) {
        DeckGLRendererV2 rendererV2 = new DeckGLRendererV2(Constants.RADIUS_IN_PIXELS, 1.0);
        rendererV2.test_project_position();
        rendererV2.test_project_common_position_to_clipspace();
        rendererV2.test_web_mercator_vewiport_project();
        rendererV2.test_different_viewports_effects_on_clipspace_positions();
    }
}
