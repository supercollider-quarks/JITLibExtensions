
NPVoicer { 
	
	var <proxy, <setParams, <synCtl, <usesSpawn = false;
	
	*new { | proxy, setParams |
		^super.newCopyArgs(proxy, setParams ? []);
	}
	
	prime { |obj, useSpawn|
		proxy.prime(obj); 
		synCtl = proxy.objects.first;
		usesSpawn = useSpawn ? usesSpawn;
		proxy.awake_(usesSpawn.not); 
		if (usesSpawn.not) { proxy.put(0, nil) };
	}
	
	put { | index, args | 
		if (proxy.awake.not) { 
			warn("NPVoicer: proxy not awake; should be awake for sustained voicing.") 
		};
		proxy.put(index, synCtl, extraArgs: args );
	}
	
	release { |key, fadeTime| proxy.removeAt(key, fadeTime) }

	releaseAll { | fadeTime | proxy.release(fadeTime) }
	
	spawn { |args| 
		if (proxy.awake) { warn("NPVoicer: proxy is awake; should not be awake for spawning.") };
		proxy.spawn(args); 
	}
	
	playingKeys { ^proxy.objects.indices }
	
		// the most basic messages for the proxy
	play { | out, numChannels, group, multi=false, vol, fadeTime, addAction |
		proxy.play(out, numChannels, group, multi=false, vol, fadeTime, addAction)
	}
	
	playN { | outs, amps, ins, vol, fadeTime, group, addAction |
		proxy.playN(outs, amps, ins, vol, fadeTime, group, addAction);
	}
	
	stop { | fadeTime = 0.1, reset = false | proxy.stop (fadeTime, reset) }
	
	end { | fadeTime = 0.1, reset = false | proxy.end(fadeTime, reset) }
	
	pause { proxy.pause }
	
	resume { proxy.resume }
}

