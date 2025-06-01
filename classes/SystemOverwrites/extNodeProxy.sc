/*
why have NodeProxy:getSpec,
and what is the preference order?

NodeProxy has two ways to define a spec for a given parameter name:
implicitly in the synth function(s), as in

Ndef(\x, { Pulse.ar(\freqx.kr(200, spec: [3, 300, \exp])) });

and the general JITLib way of using addSpec (Halo).
Ndef(\x).addSpec(\freqx, [3, 300, \exp])

As the specs for the same param name can be different in these two places,
it it is necessary to decide which one to use:
addSpec / Halo has priority over implicit specs;
if nothing is found in Halo, then we look in proxy.specs.

This precedence is useful for tuning specs while the sound continues:
once a good spec is found, one can place it back in the synth func.
If you always prefer to use implicit specs, just use implicit specs only,
or at least avoid also using addSpec for the same parameter name.

*/

+ NodeProxy {
	// convenience:
	// copy implicit specs up into halo for faster access
	addImplicitSpecsToHalo {
		this.specs.keysValuesDo { |key, value|
			this.addSpec(key, value)
		}
	}

	getSpec { |key|

		var foundSpec, synthSpecs, haloSpecs;

		// if no key given, return a dict with all specs;
		// combining synth specs and prioritized Halo specs.
		if (key.isNil) {
			synthSpecs = this.specs;
			haloSpecs = super.getSpec;
			// Halo specs will override synth specs.
			if (haloSpecs.notNil) {
				synthSpecs.putAll(haloSpecs);
			};
			^synthSpecs.parent_(Spec.specs)
		};

		// always look in halo first:
		foundSpec = super.getSpec(key);
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
