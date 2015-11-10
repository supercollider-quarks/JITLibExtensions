/*
{ { 100.rand }.value; }!5
{ { 100.rand }.valueSeed; }!5
{ { 100.rand }.valueSeed(123); }!5
*/
+ Function {
	valueSeed { |seed|
		var hasSeed = seed.notNil;
		var oldData, result;
		if (seed.isNil) {
			^this.value;
		};

		oldData = thisThread.randData;
		if(seed.isKindOf(Integer)){
			thisThread.randSeed = seed;
		}{
			thisThread.randSeed = seed.hash;
		};
		result = this.value;
		thisThread.randData_(oldData);
		^result
	}
}
