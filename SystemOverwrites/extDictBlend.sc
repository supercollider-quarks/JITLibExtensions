/*
\freq.asSpec.blend(20, 2000); // 200

a = (freq: 20, amp: 0.5, dist: 23);
b = (freq: 2000, pan: 0.2, dist: 12);

bench { 10000.do { a.blend2(b, 0.5, \left, nil, false) } }
bench { 10000.do { a.blend(b, 0.5) } };

a.blend2(b, 0.5, \sect)
a.blend2(b, 0.5, \right)
a.blend2(b, 0.5, \union)

*/

+ ControlSpec {
	blend { |val1, val2, blend = 0.5|
		^this.map(
			this.unmap(val1).blend(
				this.unmap(val2),
				blend
			)
		);
	}
}

+ Dictionary {
	// mode is one of [ \left, \right, \sect, \union ]
	blend2 { |dict2, blend = 0.5, mode = \left, specs, warnIfSpecMissing = true|
		var res;

		// if right, swap left <-> right
		if (mode == \right) {
			^dict2.blend2(this, blend, \left, specs, warnIfSpecMissing);
		};

		res = this.class.new;

		this.keysValuesDo { |key, lval|
			var newval;
			var rval = dict2[key];
			var spec = specs ? Spec.specs[key];
			if (rval.isNil) {
				if (mode != \sect) {
					res.put(key, lval)
				};
			} {
				// we have both values
				newval = if (spec.notNil) {
					spec.blend(lval, rval, blend)
				} {
					if (warnIfSpecMissing) {
						"%: no spec for key % - blending linear.\n".postf(
							thisMethod, key.cs);
					};
					lval.blend(rval, blend);
				};
				res.put(key, newval)
			}
		};
		if (mode == \union) {
			dict2.keysValuesDo { |key, rval|
				if (res[key].isNil) {
					res[key] = rval
				}
			}
		};
		^res
	}
}
