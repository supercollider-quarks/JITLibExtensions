/* overwrite getSpec:
addSpec / Halo has priority over implicit specs;
if nothing is found in Halo, look in proxy.specs.

This precedence is useful for tuning specs while sound is running:
once a good spec is found, one can place it in the synth func.
If you always prefer to use implicit specs,
just avoid using addSpec with the same parameter name.

////// Test/Show using addSpec and implicit controls:

// define an implicit spec in synthfunc:
Ndef(\x, { Pulse.ar(\freqx.kr(200, spec: [3, 300, \exp])) * 0.1 });
// it should be here:
Ndef(\x).specs;
// findFirstSpecFor should find it too
Ndef(\x).findFirstSpecFor(\freqx);
// and getSpec finds it
Ndef(\x).getSpec(\freqx);

// make sure we don't have one in the halo:
Ndef(\x).addSpec(\freqx, nil);

// use addSpec: the newly added explicit spec is used
Ndef(\x).addSpec(\freqx, [5, 500, \exp]);
Ndef(\x).getSpec(\freqx);

// when we remove the explicit spec again ..
Ndef(\x).addSpec(\freqx, nil);
// we see the implicit one again.
Ndef(\x).getSpec(\freqx);
*/

+ NodeProxy {
	getSpec { |key, value|

		// always look in halo first:
		var foundSpec = super.getSpec(key, value);
		if (foundSpec.notNil) { ^foundSpec };

		// if nothing in Halo, check in synthfunc metadata:
		// requires recent SC3 version
		if (this.respondsTo(\findFirstSpecFor)) {
			foundSpec = this.findFirstSpecFor(key);
		};
		// found or nil
		^foundSpec
	}
}
