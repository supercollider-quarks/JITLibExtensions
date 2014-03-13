ProxySubmix : Ndef {

	var <skipjack, <proxies, <sendNames, <volBusses;

	ar { |numChans(this.numChannels ? 1), addMasterSend = true|
		var res = super.ar(numChans); // initialize if not done yet
		if (addMasterSend) {
			this.put(1001, { |lev_ALL = 1|
				ReplaceOut.ar(bus, bus.ar * lev_ALL.lag(0.05));
			});
		}
		^res
	}

	addMix { |proxy, sendLevel = 0.25, postVol = true, mono = true|

		var indexInMix, sendName, volBus;
		this.checkInit;

		if (proxies.includes(proxy)) { ^this };

		indexInMix = proxies.size + 1;
		sendName = ("snd_" ++ proxy.key).asSymbol;
		proxies = proxies.add(proxy);
		sendNames = sendNames.add(sendName);
		this.addSpec(sendName, \amp);

		if (postVol) {
			if (skipjack.isNil) { this.makeSkip };
			volBus = Bus.control(server, 1);
			volBusses = volBusses.add(volBus);
		};

		this.put(indexInMix, {
			var source, levelCtl;
			source = proxy.ar;
			if (mono) { source = source.asArray.sum };
			levelCtl = sendName.kr(sendLevel);
			if (postVol) {
				levelCtl = levelCtl * volBus.kr;
			};
			source * levelCtl.lag(0.05);
		});
	}

	checkInit {
		if (proxies.isNil) {
			proxies = [];
			sendNames = [];
			volBusses = [];
			this.addSpec(\lev_ALL, [0, 4, \amp]);
		};
	}

	makeSkip {
		skipjack = SkipJack({ this.updateVols; }, 0.05);
	}

	updateVols {
		proxies.do { |proxy, i|
			var volBus = volBusses[i];
			if (volBus.notNil) { volBus.set(proxy.vol) }
		};
	}
}