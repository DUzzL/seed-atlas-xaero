package org.seedatlas.xaero.integration.marker;

import java.util.Iterator;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;

final class SeedAtlasStructureProvider
    extends ElementRenderProvider<SeedAtlasStructureMarker, SeedAtlasStructureContext> {
    private Iterator<SeedAtlasStructureMarker> iterator;

    @Override
    public void begin(ElementRenderLocation location, SeedAtlasStructureContext context) {
        this.iterator = context.markers.iterator();
    }

    @Override
    public boolean hasNext(ElementRenderLocation location, SeedAtlasStructureContext context) {
        return this.iterator != null && this.iterator.hasNext();
    }

    @Override
    public SeedAtlasStructureMarker getNext(
        ElementRenderLocation location, SeedAtlasStructureContext context
    ) {
        return this.iterator.next();
    }

    @Override
    public void end(ElementRenderLocation location, SeedAtlasStructureContext context) {
        this.iterator = null;
    }
}
