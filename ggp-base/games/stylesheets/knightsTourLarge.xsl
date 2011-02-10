
		<style type="text/css" media="all">
			#cell
			{
				width:	46px;
				height: 46px;
				float:	left;
				border: 2px solid #FFC;
				background-color: #CCCCCC;
			}
		</style>
		
		<div id="main" style="position:relative; width:400px; height:400px">
			<!-- Draw Board -->
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
					
			<!-- Draw tokens -->
			<xsl:for-each select="fact[relation='cell']">
				<xsl:variable name="x" select="50 * (./argument[1]-1)"/>
				<xsl:variable name="y" select="50 * (8-./argument[2])"/>

				<div>
					<xsl:if test="./argument[3]='knight'">
						<xsl:attribute name="style">
							<xsl:value-of select="concat('position:absolute; left:', $x ,'px; top:', $y ,'px; width:50px; height:50px;')"/>
						</xsl:attribute>		
						<img src="/docserver/gamemaster/images/wn.gif"/>
					</xsl:if>
					<xsl:if test="./argument[3]='hole'">
						<xsl:attribute name="style">
							<xsl:value-of select="concat('position:absolute; left:', $x ,'px; top:', $y ,'px; width:46px; height:46px; border: 2px solid #FFC; background-color: #CC0000;')"/>
						</xsl:attribute>
						<img src="/docserver/gamemaster/images/stop.gif"/>		
					</xsl:if>						
				</div>
			</xsl:for-each>
		</div>
		