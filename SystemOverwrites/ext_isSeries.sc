+ SequenceableCollection {

	isSeries { arg step;
		if(this.size <= 1) { ^true };
		this.doAdjacentPairs { |a, b|
			var diff = b - a;
			if(step.isNil) { step = diff } {
				if(step != diff) { ^false }
			}
		};
		^true
	}

}
