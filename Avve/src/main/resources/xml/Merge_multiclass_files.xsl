<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output encoding="UTF-8" indent="yes" method="text"/>
    <xsl:param name="classLabels"/>
    <xsl:param name="sourceFolder" select="'file:///home/kai/git/Avve/Avve/output/stats'"/>
    
    <xsl:template match="/">
        <xsl:variable name="classTokens" select="tokenize($classLabels, ',')" /> 
        <xsl:variable name="numberOfClasses" select="count(classTokens)" />
        <!-- write relation header -->
        <xsl:text>@relation ’Avve multiclass dataset: -C </xsl:text><xsl:value-of select="$numberOfClasses"/><xsl:text>’
</xsl:text>
        
        <!-- write class attributes header -->
        <xsl:for-each select="$classTokens">
            <xsl:text>@attribute </xsl:text><xsl:value-of select="."/><xsl:text> {0, 1}
</xsl:text>
        </xsl:for-each>
        
        <!-- write other attributes header -->
        <xsl:for-each select="collection(concat($sourceFolder,'?select=*.xml;recurse=yes'))[1]/dataset/header/attributes/attribute[position() > 1 and position() ne last()]">
            <xsl:text>@attribute </xsl:text><xsl:value-of select="@name"/><xsl:text> </xsl:text><xsl:value-of select="@type"/><xsl:text>
</xsl:text>
        </xsl:for-each>
        
        <xsl:text>@data</xsl:text>
        <xsl:apply-templates mode="inFile" select="collection(concat($sourceFolder,'?select=*.xml;recurse=yes'))"/>
    </xsl:template>
    
    <xsl:template mode="inFile" match="*">
        <xsl:apply-templates select="*" mode="inFile"/>
    </xsl:template>
    
    <xsl:template mode="inFile" match="instance">
        <xsl:text>% </xsl:text><xsl:value-of select="concat(base-uri(), '|', @documentId)"/><xsl:text>
</xsl:text>
        <xsl:for-each select="value[position() > 1]"><xsl:value-of select="normalize-space(.)"/><xsl:if test="position() ne last()">,</xsl:if></xsl:for-each>
        
    </xsl:template>
    
</xsl:stylesheet>