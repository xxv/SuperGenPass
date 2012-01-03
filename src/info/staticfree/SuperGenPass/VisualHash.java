package info.staticfree.SuperGenPass;

/*
 Visual Hash (SHA-1)
 Copyright (C) 2009-2011  Steve Pomeroy <steve@staticfree.info>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * <p>This displays a visual representation of the output of a hash function. Bits
 * in the hash output are mapped directly to colored shapes and their positions.</p>
 *
 * <p>This is intended to allow rapid visual identification of a known input based
 * by using spatial memory. For example, by hashing a password and displaying
 * the output to the user as they type it, they can learn to identify when they
 * have typed the password successfully by recognizing their password's distinct
 * shapes, colors and arrangements.</p>
 *
 * @author Steve Pomeroy
 *
 */
public class VisualHash extends Drawable {

	private MessageDigest mHasher;
	private byte[] mHash;

	private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

	/**
	 * <p>Make sure that any hash function you specify has an even number of bytes
	 * in the output, as each shape requires 2 bytes of data.</p>
	 *
	 * <p>This will draw n/2 shapes, where n is the number of bytes of output from
	 * the hash function. These shapes will possibly overlap and possibly be the
	 * same color as the background, so one shouldn't rely on a certain number
	 * of shapes to be visible at any given time.</p>
	 *
	 * @param hashFunction the name of the hash function, eg. "SHA-1"
	 * @throws NoSuchAlgorithmException if the specified hash algorithm can't be found.
	 */
	public VisualHash(String hashFunction) throws NoSuchAlgorithmException {
		super();
		mHasher = MessageDigest.getInstance(hashFunction);

		init();
	}

	/**
	 * Creates a new {@link VisualHash} using SHA-1.
	 */
	public VisualHash() {
		try {
			mHasher = MessageDigest.getInstance("SHA-1");
		} catch (final NoSuchAlgorithmException e) {
			mHasher = null;
			e.printStackTrace();
		}

		init();
	}

	private void init(){
		p.setStyle(Style.FILL);
		p.setStrokeWidth(0);
		setData("".getBytes());
	}

	/**
	 * Sets the data and digests it into the visual hash.
	 *
	 * @param input the data to be hashed. This value is discarded immediately after computing the hash.
	 */
	public void setData(byte[] input) {
		if (mHasher == null){
			mHash = new byte[2];
			return;
		}
		mHasher.update(input);
		mHash = mHasher.digest();
		invalidateSelf();
	}

	private static final int

	TYPE_CIRCLE = 0x00, TYPE_SQUARE = 0x01, TYPE_STAR = 0x02,
			TYPE_TRIANGLE = 0x03;

	// the below defines the offsets to pull the bits of the hash out into
	// visual characteristics.
	private static final int BYTES_PER_SHAPE = 2,

	TYPEMASK = 0x3, TYPEBYTE = 0,

	// this creates a grid of 4x4 potential shapes.
			X_OFFSET = 3, X_MAX = 0x3, X_MASK = X_MAX << X_OFFSET, X_BYTE = 0,

			Y_OFFSET = 6, Y_MAX = 0x3, Y_MASK = Y_MAX << Y_OFFSET, Y_BYTE = 0,

			R_OFFSET = 0, R_MAX = 0x3, R_MASK = R_MAX << R_OFFSET, R_BYTE = 1,

			G_OFFSET = 3, G_MAX = 0x3, G_MASK = G_MAX << G_OFFSET, G_BYTE = 1,

			B_OFFSET = 6, B_MAX = 0x2, B_MASK = B_MAX << B_OFFSET, B_BYTE = 1;

	private final static int SPACING = 2, RADIUS = 8, // This is a constant
														// based on the static
														// shape definitions.
			ORIGIN_OFFSET = RADIUS + SPACING / 2, // amount to move 0,0 so that
													// drawing a circle with the
													// given radius will be
													// visible

			PRESCALE_CENTER_WIDTH = (RADIUS * 2 + SPACING) * 3, // pixel width;
																// centers are 3
																// shapes wide
			PRESCALE_WIDTH = (RADIUS * 2 + SPACING) * 4; // width is 4 shapes
															// wide

	private final static Path TRIANGLE = new Path();
	private final static Path STAR = new Path();

	private final static Paint PAINT_IT_RED = new Paint(Paint.ANTI_ALIAS_FLAG);

	static {
		TRIANGLE.moveTo(-RADIUS, RADIUS);
		TRIANGLE.lineTo(RADIUS, RADIUS); // _
		TRIANGLE.lineTo(0, -RADIUS);
		TRIANGLE.lineTo(-RADIUS, RADIUS);

		// star is based on radius size 8
		STAR.lineTo(0.000000f, -8.475681f);
		STAR.lineTo(1.893601f, -2.597389f);
		STAR.lineTo(8.069343f, -2.612960f);
		STAR.lineTo(3.063910f, 1.004453f);
		STAR.lineTo(4.987128f, 6.873122f);
		STAR.lineTo(0.000000f, 3.230514f);
		STAR.lineTo(-4.987129f, 6.873122f);
		STAR.rLineTo(1.923218f, -5.868669f);
		STAR.rLineTo(-5.005433f, -3.617414f);
		STAR.rLineTo(6.175743f, 0.015571f);
		STAR.lineTo(0.000000f, -8.475681f);
		PAINT_IT_RED.setStyle(Style.STROKE);
		PAINT_IT_RED.setColor(Color.RED);
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		mWidth = bounds.width();
		mHeight = bounds.height();
		scaleX = mWidth / (float) PRESCALE_WIDTH;
		scaleY = mHeight / (float) PRESCALE_WIDTH;

	}

	private int mWidth, mHeight;
	private float scaleX, scaleY;

	@Override
	public void draw(Canvas canvas) {

		canvas.scale(scaleX, scaleY);
		for (int offset = 0; offset < (mHash.length); offset += BYTES_PER_SHAPE) {
			final int type = mHash[TYPEBYTE + offset] & TYPEMASK;
			final int x = (mHash[X_BYTE + offset] & X_MASK) >> X_OFFSET;
			final int y = (mHash[Y_BYTE + offset] & Y_MASK) >> Y_OFFSET;

			p.setARGB(
					200,
					scaleInt(R_MAX,
							(mHash[R_BYTE + offset] & R_MASK) >> R_OFFSET, 255),
					scaleInt(G_MAX,
							(mHash[G_BYTE + offset] & G_MASK) >> G_OFFSET, 255),
					scaleInt(B_MAX,
							(mHash[B_BYTE + offset] & B_MASK) >> B_OFFSET, 255));
			final float xCenterScaled = ORIGIN_OFFSET
					+ scale(X_MAX, x, PRESCALE_CENTER_WIDTH);
			final float yCenterScaled = ORIGIN_OFFSET
					+ scale(Y_MAX, y, PRESCALE_CENTER_WIDTH);
			canvas.save();
			canvas.translate(xCenterScaled, yCenterScaled);

			switch (type) {
			case TYPE_STAR:
				canvas.drawPath(STAR, p);
				break;
			case TYPE_CIRCLE:

				canvas.drawCircle(0, 0, RADIUS, p);
				break;

			case TYPE_TRIANGLE:

				canvas.drawPath(TRIANGLE, p);
				break;
			case TYPE_SQUARE:

				canvas.drawRect(-RADIUS, -RADIUS, RADIUS, RADIUS, p);
				break;
			}
			canvas.restore();
		}
	}

	private int scaleInt(int valueMax, int value, int max) {
		return (int) ((value / ((float) valueMax)) * max);
	}

	private float scale(int valueMax, int value, int max) {
		return (value / ((float) valueMax)) * max;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

	@Override
	public void setAlpha(int arg0) {
	}

	@Override
	public void setColorFilter(ColorFilter arg0) {
	}

	@Override
	public int getIntrinsicWidth() {
		return PRESCALE_WIDTH;
	}

	@Override
	public int getIntrinsicHeight() {
		return PRESCALE_WIDTH;
	}
}
