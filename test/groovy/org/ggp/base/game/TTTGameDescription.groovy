package org.ggp.base.game

class TTTGameDescription {
	private String ruleSheet = '(' +
	' ( role white )' +
	' ( role black )' +
	' ( <= ( base ( cell ?M ?N x ) ) ( index ?M ) ( index ?N ) )' +
	' ( <= ( base ( cell ?M ?N o ) ) ( index ?M ) ( index ?N ) )' +
	' ( <= ( base ( cell ?M ?N b ) ) ( index ?M ) ( index ?N ) )' +
	' ( base ( control white ) )' +
	' ( base ( control black ) )' +
	' ( <= ( input ?R ( mark 2 1 ) ) ( role ?R ) )' +
	' ( <= ( input ?R ( mark 2 3 ) ) ( role ?R ) )' +
	' ( <= ( input ?R ( mark 3 1 ) ) ( role ?R ) )' +
	' ( <= ( input ?R noop ) ( role ?R ) )' +
	' ( index 1 )' +
	' ( index 2 )' +
	' ( index 3 )' +
	' ( init ( cell 1 1 b ) )' +
	' ( init ( cell 1 2 b ) )' +
	' ( init ( cell 1 3 b ) )' +
	' ( init ( cell 2 1 b ) )' +
	' ( init ( cell 2 2 b ) )' +
	' ( init ( cell 2 3 b ) )' +
	' ( init ( cell 3 1 b ) )' +
	' ( init ( cell 3 2 b ) )' +
	' ( init ( cell 3 3 b ) )' +
	' ( init ( control white ) )' +
	' ( <= ( legal ?W ( mark ?X ?Y ) ) ( true ( cell ?X ?Y b ) ) ( true ( control ?W ) ) )' +
	' ( <= ( legal white noop ) ( true ( control black ) ) )' +
	' ( <= ( legal black noop ) ( true ( control white ) ) )' +
	' ( <= ( next ( cell ?M ?N x ) ) ( does white ( mark ?M ?N ) ) ( true ( cell ?M ?N b ) ) )' +
	' ( <= ( next ( cell ?M ?N o ) ) ( does black ( mark ?M ?N ) ) ( true ( cell ?M ?N b ) ) )' +
	' ( <= ( next ( cell ?M ?N ?W ) ) ( true ( cell ?M ?N ?W ) ) ( distinct ?W b ) )' +
	' ( <= ( next ( cell ?M ?N b ) ) ( does ?W ( mark ?J ?K ) ) ( true ( cell ?M ?N b ) ) ( or ( distinct ?M ?J ) ( distinct ?N ?K ) ) )' +
	' ( <= ( next ( control white ) ) ( true ( control black ) ) )' +
	' ( <= ( next ( control black ) ) ( true ( control white ) ) )' +
	' ( <= ( row ?M ?X ) ( true ( cell ?M 1 ?X ) ) ( true ( cell ?M 2 ?X ) ) ( true ( cell ?M 3 ?X ) ) )' +
	' ( <= ( column ?N ?X ) ( true ( cell 1 ?N ?X ) ) ( true ( cell 2 ?N ?X ) ) ( true ( cell 3 ?N ?X ) ) )' +
	' ( <= ( diagonal ?X ) ( true ( cell 1 1 ?X ) ) ( true ( cell 2 2 ?X ) ) ( true ( cell 3 3 ?X ) ) )' +
	' ( <= ( diagonal ?X ) ( true ( cell 1 3 ?X ) ) ( true ( cell 2 2 ?X ) ) ( true ( cell 3 1 ?X ) ) )' +
	' ( <= ( line ?X ) ( row ?M ?X ) )' +
	' ( <= ( line ?X ) ( column ?M ?X ) )' +
	' ( <= ( line ?X ) ( diagonal ?X ) )' +
	' ( <= open ( true ( cell ?M ?N b ) ) )' +
	' ( <= ( goal white 100 ) ( line x ) ( not ( line o ) ) )' +
	' ( <= ( goal white 50 ) ( line x ) ( line o ) )' +
	' ( <= ( goal white 50 ) ( not ( line x ) ) ( not ( line o ) ) )' +
	' ( <= ( goal white 0 ) ( not ( line x ) ) ( line o ) )' +
	' ( <= ( goal black 100 ) ( not ( line x ) ) ( line o ) )' +
	' ( <= ( goal black 50 ) ( line x ) ( line o ) )' +
	' ( <= ( goal black 50 ) ( not ( line x ) ) ( not ( line o ) ) )' +
	' ( <= ( goal black 0 ) ( line x ) ( not ( line o ) ) )' +
	' ( <= terminal ( line x ) )' +
	' ( <= terminal ( line o ) )' +
	' ( <= terminal ( not open ) )' +
	' )'

	String getRuleSheet() {
		ruleSheet
	}
}
