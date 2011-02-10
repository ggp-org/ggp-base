<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
<html style="width:600px; height:600px ">
<head>
<title><xsl:value-of select="match/@id"/></title>
<style type="text/css" media="all">			
			.mainbox > div
			{
				margin: auto;
			}
		</style>
</head>
<body leftmargin='0' topmargin='0' marginwidth='0' marginheight='0' style="width:100%; height:100%" bgcolor='#ffffcc'>

<div style="display: table; position: static; height: 100%; width: 100%">
<xsl:for-each select="match/herstory/state[###STATE_NUM_HERE###]">
  <div class="mainbox" style='display: table-cell; vertical-align: middle; position: static; width: 100%'>
    <xsl:attribute name='id'><xsl:value-of select="stepnum"/></xsl:attribute>

		###GAME_SPECIFIC_STUFF_HERE###
		
  </div>
</xsl:for-each>
</div>

</body>
</html>
</xsl:template>
</xsl:stylesheet>