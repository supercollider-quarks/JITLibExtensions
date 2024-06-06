MainFX {

	classvar <all, <maxNumChans = 8;
	var <group, <numChannels, <busIndex, <server, <proxyChain;
	var <checkingBadValues = true, <badSynth, badDefName;

	*initClass {
		all = IdentityDictionary.new;
	}

	*default { ^all[Server.default.name] }


	// only one MainFX per server ATM.
	// This could be changed if different MainFX
	// for different groups of output channels are to be used.

	*new { |server, numChannels, slotNames, busIndex|
		var serverName, fx;
		server = server ?? { Server.default };
		case { server.isKindOf(Server) } {
			serverName = server.name
		} { server.isKindOf(Symbol) } {
			serverName = server;
			server = Server.named[serverName];
			if (server.isNil) {
				"*** MainFX: could not find server for % !\n".postf(serverName.cs);
				^nil
			}
		};

		fx = all[serverName];

		if (fx.notNil) {
			if ( [numChannels, slotNames, busIndex].any(_.notNil) ) {
				"// MainFX for server % exists, cannot change its settings while running - use\n"
				"MainFX.clear(%) \n// to make a new one.\n"
				.postf(server.name, server.name.asCompileString);
			};
			^fx
		} {
			// "MainFX - going into make".postln;
			^this.make(server, numChannels, slotNames, busIndex ? 0)
		}
	}

	*make { |server, numChannels, slotNames|
		^super.new.init2(server, numChannels, slotNames);
	}

	makeBus {
		^Bus.new(\audio, busIndex, numChannels, server);
	}

	key { ^all.findKeyForValue(this) }
	storeArgs { ^[server.name] }
	printOn { |stream| ^this.storeOn(stream) }

	// interface to proxyChain

	add { |key, wet, func|
		proxyChain.add(key, wet, func);
	}
	remove { |key|
		proxyChain.remove(key);
	}

	set { |...args| proxyChain.set(*args) }

	slotNames { ^proxyChain.slotNames }

	slotNames_ { |argSlotNames| proxyChain.slotNames_(argSlotNames) }

	proxy { ^proxyChain.proxy }

	pxChain { ^proxyChain } // backwards compatibility

	slotsInUse { ^proxyChain.slotsInUse }

	// hide Ndef by default
	hide {
		Ndef.all[server.name].envir.removeAt(proxyChain.proxy.key);
	}
	// show it in case that is useful in some circumstances?
	show {
		Ndef.all[server.name].envir.put(proxyChain.proxy.key, proxyChain.proxy);
	}

	init2 { |inServer, inNumChannels, inSlotNames, inBusIndex|
		var proxy;
		server = inServer ? Server.default;
		numChannels = inNumChannels ? server.options.numOutputBusChannels;
		busIndex = inBusIndex ? 0;

		proxy = Ndef(\mainFX -> server.name);
		proxy.ar(numChannels);
		proxy.bus_(this.makeBus);
		proxyChain = ProxyChain.from(proxy, inSlotNames ? []);

		this.hide;	// hide by default

		all.put(server.name, this);

		this.makeGroup;
		// "mainfx.init".postln;
		ServerTree.add(this, server);

		checkingBadValues = \Safety.asClass.isNil;

		if (checkingBadValues) {
			badDefName = ("BadMainFX_" ++ server.name).asSymbol;
			SynthDef(badDefName, {
				var snd = In.ar(busIndex, numChannels);
				var dt = 0.001;
				var isOK = (CheckBadValues.ar(snd) < 0.001);
				var gate = (isOK * DelayN.ar(isOK, dt * 2));
				var outSnd = 	DelayL.ar(snd, dt) * gate;
				ReplaceOut.ar(busIndex, outSnd)
			}).add;
		}
	}

	makeGroup {
		group = Group.new(server.defaultGroup, \addAfter).isPlaying_(true);
		proxyChain.proxy.parentGroup_(group);
	}

	doOnServerTree { this.wakeUp }

	wakeUp {
		fork {
			this.makeGroup;
			server.sync; // make sure new groups are in place before rebuild
			proxyChain.proxy.rebuild.send;
			if (checkingBadValues) { this.checkBad };
			"\MainFX for % woke up.\n\n".postf(this, server.name);
		}
	}

	clear {
		ServerTree.remove(this, server);
		proxyChain.proxy.clear;
		all.removeAt(proxyChain.proxy.server.name);
	}

	*clear { |name|
		(name ?? { all.keys }).do { |name|
			all.removeAt(name).clear;
		};
	}

	makeName {
		^(this.class.name ++ "_" ++ server.name
			++ "_" ++ proxyChain.proxy.numChannels).asSymbol
	}

	gui { |name, numItems, buttonList, parent, bounds, makeSkip = true|
		// the effects are all on by default:
		buttonList = buttonList ?? { proxyChain.slotNames.collect ([_, \slotCtl]) };
		name = name ?? { this.makeName };
		numItems = numItems ? 16;
		^MainFXGui(this, numItems, parent, bounds, makeSkip, buttonList)
		.name_(name);
	}

	checkBad { |flag|
		checkingBadValues = flag ? checkingBadValues;
		badSynth !? {
			// free badSynth - which may be gone already
			// so suppress error msg for this bundle
			server.sendBundle(nil, [\error, -1], ["/free", badSynth.nodeID]);
		};

		if (checkingBadValues) {
			badSynth = Synth(badDefName, target: group, addAction: \addAfter);
		};
	}

}

MasterFX : MainFX {
	*new { |server, numChannels, slotNames, busIndex|
		"MasterFX has been renamed MainFX, please adapt your code accordingly.".postln;
		^MainFX.new(server, numChannels, slotNames, busIndex)
	}
}
