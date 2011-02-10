
		
		<!-- Draw Board -->
		<div id="main" style="position:relative; width:400px; height:400px">
			<img src="/docserver/gameserver/images/Qyshinsu/board.png"/>
			
			<xsl:for-each select="fact[relation='position']">
				<xsl:if test="./argument[2]!='empty'">
								
					<xsl:variable name="pos" select="./argument[1]"/>
					<xsl:variable name="owner" select="../fact[relation='owner' and argument[1]=$pos]/argument[2]"/>
					
					<xsl:variable name="value" select="./argument[2]"/>
					
					<img src="/docserver/gameserver/images/Qyshinsu/R0.png">
					<xsl:attribute name="src">
						<xsl:if test="$owner='red'"><xsl:value-of select="concat('/docserver/gameserver/images/Qyshinsu/R',$value,'.png')"/></xsl:if>
						<xsl:if test="$owner='black'"><xsl:value-of select="concat('/docserver/gameserver/images/Qyshinsu/B',$value,'.png')"/></xsl:if>						
					</xsl:attribute>
					<xsl:attribute name="style">
					<!-- TABLE OF POSITIONS -->
						<xsl:if test="$pos=1">
							<xsl:value-of select="'position:absolute; left: 250px; top: 65px'"/>							
						</xsl:if>
						<xsl:if test="$pos=2">
							<xsl:value-of select="'position:absolute; left: 285px; top: 130px'"/>							
						</xsl:if>
						<xsl:if test="$pos=3">
							<xsl:value-of select="'position:absolute; left: 290px; top: 190px'"/>							
						</xsl:if>
						<xsl:if test="$pos=4">
							<xsl:value-of select="'position:absolute; left: 270px; top: 245px'"/>							
						</xsl:if>
						<xsl:if test="$pos=5">
							<xsl:value-of select="'position:absolute; left: 210px; top: 285px'"/>							
						</xsl:if>
						<xsl:if test="$pos=6">
							<xsl:value-of select="'position:absolute; left: 150px; top: 285px'"/>							
						</xsl:if>
						<xsl:if test="$pos=7">
							<xsl:value-of select="'position:absolute; left: 95px; top: 265px'"/>							
						</xsl:if>
						<xsl:if test="$pos=8">
							<xsl:value-of select="'position:absolute; left: 55px; top: 215px'"/>							
						</xsl:if>
						<xsl:if test="$pos=9">
							<xsl:value-of select="'position:absolute; left: 40px; top: 145px'"/>							
						</xsl:if>
						<xsl:if test="$pos=10">
							<xsl:value-of select="'position:absolute; left: 70px; top: 95px'"/>							
						</xsl:if>
						<xsl:if test="$pos=11">
							<xsl:value-of select="'position:absolute; left: 120px; top: 55px'"/>							
						</xsl:if>
						<xsl:if test="$pos=12">
							<xsl:value-of select="'position:absolute; left: 185px; top: 40px'"/>							
						</xsl:if>
					</xsl:attribute>
					</img>
										
				</xsl:if>					
			</xsl:for-each>
		</div>
		
		