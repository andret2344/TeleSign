package eu.andret.telesign.helper;

import org.bukkit.block.Block;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.sign.Side;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.mockbukkit.mockbukkit.block.state.SignStateMock;

/**
 * A MockBukkit standing sign that implements {@link #getInteractableSideFor(double, double)}, which MockBukkit leaves
 * unimplemented: the front is the side the sign is rotated towards, as on the server.
 */
public class SidedSignStateMock extends SignStateMock {
	public SidedSignStateMock(@NotNull final Block block) {
		super(block);
	}

	private SidedSignStateMock(@NotNull final SidedSignStateMock state) {
		super(state);
	}

	@NotNull
	@Override
	public Side getInteractableSideFor(final double x, final double z) {
		final Vector direction = ((Rotatable) getBlockData()).getRotation().getDirection();
		final double dx = x - (getX() + 0.5);
		final double dz = z - (getZ() + 0.5);
		return dx * direction.getX() + dz * direction.getZ() >= 0 ? Side.FRONT : Side.BACK;
	}

	@NotNull
	@Override
	public SidedSignStateMock getSnapshot() {
		return new SidedSignStateMock(this);
	}
}
