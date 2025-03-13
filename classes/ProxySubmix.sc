// temporary solution for postfader submixing
// when/if proxies use volume busses, this will change.

ProxySubmix : Ndef {

	var <skipjack, collection;

	addLevel { |lev_ALL = 1, masterNodeID = 1001|
		// if needed, init with default numChannels from NodeProxy
		if (this.isNeutral) { this.ar };

		this.put(masterNodeID, {
			ReplaceOut.ar(bus,
				bus.ar * \lev_ALL.kr(lev_ALL).lag(0.05)
			);
		});
	}

	addMix { |proxy, sendLevel = 0.25, postVol = true, mono = false|

		var objIndex, item, sendName, volBus;
		this.checkInit(proxy);

		if (collection.any { |dict| dict[\proxy] === proxy }) {
			"%: already has %\n".postf(this, proxy);
			^this
		};

		objIndex = collection.collect(_[\objIndex]).maxItem ? 0 + 1;
		sendName = ("snd_" ++ proxy.key).asSymbol;
		item = (proxy: proxy, name: sendName, objIndex: objIndex);
		collection = collection.add(item).postln;
		this.addSpec(sendName, \amp);

		if (postVol) {
			if (skipjack.isNil) { this.makeSkip };
			volBus = Bus.control(server, 1);
			item[\volBus] = volBus;
		};

		this.put(objIndex, {
			var source, levelCtl;
			source = NumChannels.ar(proxy.ar,
				if(mono) { 1 } { this.numChannels }
			);
			levelCtl = sendName.kr(sendLevel);
			if (postVol) {
				levelCtl = levelCtl * volBus.kr;
			};
			source * levelCtl.lag(0.05);
		});
		proxy.addDependant(this);
	}
	removeMix { |proxy|
		var proxyDict = collection.detect { |item| item[\proxy] === proxy };
		if(proxyDict.notNil) {
			proxyDict[\proxy].removeDependant(this);
			proxyDict[\volBus].free;
			this.put(proxyDict.objIndex, nil);
			collection.remove(proxyDict);
		};
	}

	checkInit { |proxy|
		if (this.isNeutral) { this.ar(proxy.numChannels) };

		if (collection.isNil) {
			collection = [];
			this.addSpec(\lev_ALL, [0, 4, \amp]);
		};
	}

	makeSkip {
		skipjack = SkipJack({ this.updateVols; }, 0.05);
	}

	updateVols {
		// collect all setmessages and send as one bundle
		// to reduce osc traffic
		server.bind {
			collection.do { |item, i|
				var volBus;
				if(item.notNil) {
					volBus = item[\volBus];
					if (volBus.notNil) {
						volBus.set(
							item[\proxy].vol
							* item[\proxy].monitor.isPlaying.binaryValue
						)
					};
				};
			};
		};
	}

	clear {
		collection.do { |item|
			if(item.notNil) {
				item[\proxy].removeDependant(this);
				item[\volBus].free;
			};
		};
		collection.clear;

		skipjack.stop;
		skipjack = nil;
		^super.clear;
	}

	proxies { ^collection.collect { |item| item[\proxy] } }
	sendNames { ^collection.collect { |item| item[\name] } }
}