package com.cluck.client;

import com.cluck.entity.PurgeDrone;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public final class PurgeDroneRenderer extends ThrownItemRenderer<PurgeDrone> {
	public PurgeDroneRenderer(EntityRendererProvider.Context context) {
		super(context, 1.0F, true);
	}
}
