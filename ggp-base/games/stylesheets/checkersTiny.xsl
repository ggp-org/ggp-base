
				<style type="text/css" media="all">
					#cell
					{
						width:  48px;
						height: 48px;
						float:	left;
						border: 1px solid #555;
						background-color: #D4D0C8;
			            text-align: center;
			            font-size: 40px;
			            font-weight: bold;
					}
          #board
          {
						width:  400px;
						height: 400px;
						float:	left;
            			margin: 4px;
          }
          #valid
          {
            color: #000;
          }      
				</style>


        <div id="board">
            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='2' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='2' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='2' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='2' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='4' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='4' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='4' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='4' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='6' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='6' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='6' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='6' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='8' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='8' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='8' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='a' and argument[2]='8' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='1' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='1' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='1' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='1' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='3' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='3' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='3' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='3' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='5' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='5' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='5' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='5' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='7' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='7' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='7' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='b' and argument[2]='7' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='2' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='2' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='2' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='2' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='4' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='4' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='4' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='4' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='6' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='6' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='6' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='6' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='8' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='8' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='8' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='c' and argument[2]='8' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='1' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='1' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='1' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='1' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='3' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='3' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='3' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='3' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='5' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='5' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='5' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='5' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>

    	    <div id="cell">
    	      <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='7' and argument[3]='wp']"><div id="valid"><img src="images/Checkers/red_pawn.png"></img></div></xsl:if>
    	      <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='7' and argument[3]='wk']"><div id="valid"><img src="images/Checkers/red_king.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='7' and argument[3]='bp']"><div id="valid"><img src="images/Checkers/black_pawn.png"></img></div></xsl:if>
              <xsl:if test="fact[relation='cell' and argument[1]='d' and argument[2]='7' and argument[3]='bk']"><div id="valid"><img src="images/Checkers/black_king.png"></img></div></xsl:if>
            </div>

            <div id="cell">
            	<div id="valid"><img src="images/Checkers/empty_blocked.png"></img></div>
            </div>          
        </div>

		