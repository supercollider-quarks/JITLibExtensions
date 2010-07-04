	// move to regular JITLib when fully tested. 
	
+ NodeProxy { 
	setAt { |nodeKey, args|
		var node = this.objects[nodeKey];
		if (node.notNil) { node.set(*args) }
	}
}

+ SynthControl { 
	set { | ... args | 
		// as fast as possible, no errors please
		server.sendBundle(nil, [ 
			['/error', -1], 
			["/n_set", nodeID] ++ args,
			['/error', -2] 
		]);
	}
}

