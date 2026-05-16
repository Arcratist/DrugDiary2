package brettdansmith.drugdiary.domain.model.resources;

import java.util.List;

public final class SupportResourceProvider {
    private SupportResourceProvider() {}

    public static List<SupportResource> getResources() {
        return SupportResourceRegistry.all();
    }
}
