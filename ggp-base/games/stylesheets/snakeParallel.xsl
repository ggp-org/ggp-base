
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
		
		<div id="main" style="position:relative; width:400px; height:200px">
			<!-- Draw Board -->
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
			<div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/><div id="cell"/>
					
			<!-- Draw tokens -->
			<xsl:for-each select="fact[relation='cell']">
				<xsl:variable name="x" select="50 * (./argument[2] - 1 + ((./argument[1]-1) * 4)) + 2"/>
				<xsl:variable name="y" select="50 * (4-./argument[3]) + 2"/>

				<div>
					<xsl:if test="./argument[4]='snake'">
						<xsl:attribute name="style">
							<xsl:value-of select="concat('position:absolute; left:', $x ,'px; top:', $y ,'px; width:46px; height:46px; background-color:#CC0000')"/>
						</xsl:attribute>		
					</xsl:if>
					<xsl:if test="./argument[4]='body'">
						<xsl:attribute name="style">
							<xsl:value-of select="concat('position:absolute; left:', $x ,'px; top:', $y ,'px; width:46px; height:46px; background-color:#CCCC00')"/>
						</xsl:attribute>		
					</xsl:if>
				</div>
			</xsl:for-each>
		</div>
		