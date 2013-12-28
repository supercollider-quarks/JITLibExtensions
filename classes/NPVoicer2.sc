
NPVoicer2 : NPVoicer {
	var <>limitVoices = true, <>maxVoices = 16, <voiceHistory, <>stealMode = \oldest;
	var <defParamValues;

	// in NPVoicer:prime, check whether synthdef hasGate or not;
	// if not, schedule removal of by sustain
	// if yes, release hould remove it

	prime { |obj, useSpawn|
		super.prime(obj, useSpawn);
		defParamValues = ();
		synthDesc.controlDict.keysValuesDo { |parName, control|
			defParamValues.put(parName, control.defaultValue);
		};
		voiceHistory = List[];
	}

	put { |key, args|
		super.put(key, args);
		this.removeVoiceAt(key); // super releases earlier voice under that key
		this.checkLimit(key, args);
		this.trackVoice(key, args);
	}

	release {|key|
		super.release(key);
		this.removeVoiceAt(key);
	}

	removeVoiceAt { |key|
		var voiceHistIndex;
		voiceHistIndex = voiceHistory.detectIndex { |ev| ev[0] == key };
		voiceHistIndex !? { voiceHistory.removeAt(voiceHistIndex); };
	}

	releaseAll {
		super.releaseAll;
		voiceHistory.clear;
	}

	cmdPeriod { voiceHistory.clear.postln; }

	postHist { voiceHistory.printAll; }

	trackVoice {|key, args|
		voiceHistory.add([key, args]);
		// why use a voicer when notes end by themselves?
		// maybe ask proxy who is still playing every now and then
		if (hasGate.not) {
			// figure how to estimate time synth will live
			var susDefault = defParamValues[\sustain];
			var susArgIndex = args.indexOf(\sustain);
			var susFromArg = susArgIndex !? { args[susArgIndex + 1] };
			var soundingTime = susFromArg ? susDefault ? 1;
			defer ({ this.release(key) }, soundingTime);
		};
	}

	findSoftestIndex {
		var minAmp = 1000, minIndex = nil;
		var defAmp = defParamValues[\amp];

		if (defAmp.isNil) { ^nil };

		voiceHistory.do { |ev, evi|
			var ampSymi, ampVali, ampVal = defAmp;
			ampSymi = ev[1].indexOf(\amp);

			if (ampSymi.notNil) {
				ampVali = ampSymi + 1;
				ampVal = ev[1][ampVali]
			};
			if (ampVal < minAmp) {
				minAmp = ampVal;
				minIndex = evi
			};
		};
		"findSoftest: minAmp: %, minIndex: %\n".postf(minAmp, minIndex);

		^minIndex
	}

	checkLimit {
		// check before adding the new voice,
		// so it can never be killed
		if (proxy.objects.size <= maxVoices) { ^this };

		stealMode.switch(
			\oldest, { this.release(voiceHistory[0][0]) },
			\lowest, { this.release(proxy.objects.indices[0]) },
			// maybe top and bottom voices will be less dispensable?
			\middle, {
				var keys = proxy.objects.indices;
				var key = keys[keys.size div: 2];
				this.release(key);
			},
			\softest, {
				var index = this.findSoftestIndex ? 0;
				this.release(voiceHistory[index][0]);
			},
			{ this.release(voiceHistory[0][0]) }
		);
	}
}