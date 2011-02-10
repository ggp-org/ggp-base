
				<style type="text/css" media="all">
					#cell
					{
						width:  46px;
						height: 46px;
						float:	left;
						border: 2px solid #555;
						background-color: #999;
            text-align: center;
            font-size: 40px;
            font-weight: bold;
					}
          #board
          {
						width:  150px;
						height: 150px;
						float:	left;
            margin: 4px;
          }
          #boards
          {
            width:    474px;
            height:   474px;
            padding: 4px;
            position: relative;
						background-color: #ffc;
          }
          #x
          {
            color: #000;
          }
          #o
          {
            color: #fff;
          }
				</style>


        <div id="boards">

          <div id="board">
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='1' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='1' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='1' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='1' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='1' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='1' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='2' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='2' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='2' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='2' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='2' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='2' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='3' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='3' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='3' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='3' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='3' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='1' and argument[3]='3' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
          </div>

          <div id="board">
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='1' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='1' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='1' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='1' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='1' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='1' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='2' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='2' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='2' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='2' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='2' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='2' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='3' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='3' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='3' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='3' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='3' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='2' and argument[3]='3' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
          </div>

          <div id="board">
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='1' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='1' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='1' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='1' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='1' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='1' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='2' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='2' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='2' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='2' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='2' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='2' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='3' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='3' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='3' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='3' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='3' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='1' and argument[2]='3' and argument[3]='3' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
          </div>

          <div id="board">
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='1' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='1' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='1' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='1' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='1' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='1' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='2' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='2' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='2' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='2' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='2' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='2' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='3' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='3' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='3' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='3' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='3' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='1' and argument[3]='3' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
          </div>

          <div id="board">
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='1' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='1' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='1' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='1' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='1' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='1' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='2' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='2' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='2' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='2' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='2' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='2' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='3' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='3' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='3' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='3' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='3' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='2' and argument[3]='3' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
          </div>

          <div id="board">
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='1' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='1' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='1' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='1' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='1' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='1' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='2' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='2' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='2' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='2' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='2' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='2' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='3' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='3' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='3' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='3' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='3' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='2' and argument[2]='3' and argument[3]='3' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
          </div>

          <div id="board">
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='1' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='1' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='1' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='1' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='1' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='1' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='2' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='2' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='2' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='2' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='2' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='2' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='3' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='3' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='3' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='3' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='3' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='1' and argument[3]='3' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
          </div>

          <div id="board">
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='1' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='1' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='1' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='1' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='1' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='1' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='2' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='2' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='2' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='2' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='2' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='2' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='3' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='3' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='3' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='3' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='3' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='2' and argument[3]='3' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
          </div>

          <div id="board">
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='1' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='1' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='1' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='1' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='1' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='1' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='2' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='2' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='2' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='2' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='2' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='2' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='3' and argument[4]='1' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='3' and argument[4]='1' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='3' and argument[4]='2' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='3' and argument[4]='2' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
            <div id="cell">
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='3' and argument[4]='3' and argument[5]='x']"><div id="x">X</div></xsl:if>
              <xsl:if test="fact[relation='mark' and argument[1]='3' and argument[2]='3' and argument[3]='3' and argument[4]='3' and argument[5]='o']"><div id="o">O</div></xsl:if>
            </div>
          </div>

        </div>

		