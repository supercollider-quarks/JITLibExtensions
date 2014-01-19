// repair a few annoyances in JITGuis for 3.6.6 and earlier versions,
// by overwriting the faulty methods with those from regular 3.7.0.
// remove the overwrites again when 3.6.6 is obsolete.

// enable TaskProxyGuis for anonymous tasks,
// and let TdefGuis et al be more generous


+ TaskProxyGui {
	*observedClass { ^PatternProxy }
	accepts { |obj| ^obj.isNil or: { obj.isKindOf(PatternProxy) } }
}
+ PatternProxy {
	key { ^'anon' }
}

// fix invisible buttons on TdefGuis
// - offColor was Color.clear, now Color.grey(0.8, 0.5).
+ JITGui {
	*initClass {
		Class.initClassTree(GUI);

		GUI.skins.put(\jit, (
				fontSpecs: 	["Helvetica", 12],
				fontColor: 	Color.black,
				background: 	Color(0.8, 0.85, 0.7, 0.5),
				foreground:	    Color.grey(0.95),
				onColor:		Color(0.5, 1, 0.5),
				offColor:		Color.grey(0.8, 0.5),
				hiliteColor:	Color.green(1.0, 0.5),
				gap:			0 @ 0,
				margin: 		2@2,
				buttonHeight:	18,
				headHeight: 	24

			)
		);
	}

}
   // fix broken audio
+ MonitorGui {
	getState {
		var isAudio, newState;
		var monitor, outs, amps, newHasSeriesOut;
		var plays = 0, playsSpread = false;

		newState = (
			object: nil,
			name: \_none_,
			isAudio: false,
			monPlaying: 	0,
			vol: 		1,
			usedPlayN: 	false,
			playsSpread: 	false,
			out: 		0,
			monFade:		0.02
		);

		if (object.isNil) { ^newState };

		// still get the proper info if server is off,
		// but Ndef has an audio synthfunc
		isAudio = (object.rate == \audio) or: {
			if (object.objects.notEmpty) {
				object.objects.array.first.synthDef.rate == \audio;
			};
		};

		newState.putPairs([
			\object, object,
			\name, object.key,
			\isAudio, isAudio
		]);

		monitor = object.monitor;
		if (monitor.isNil) {
			^newState
		};


		newState.putPairs([
			\monPlaying,	monitor.isPlaying.binaryValue,
			\vol, 		monitor.vol,
			\usedPlayN, 	monitor.usedPlayN,
			\out, 		monitor.out ? 0,
			\playsSpread,	monitor.hasSeriesOuts.not,
			\monFade,		monitor.fadeTime
		]);

		^newState;
	}
}
