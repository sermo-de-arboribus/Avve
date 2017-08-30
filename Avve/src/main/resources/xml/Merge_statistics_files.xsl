<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output encoding="UTF-8" indent="yes"/>
    <xsl:param name="sourceFolder" select="'file:///home/kai/git/Avve/Avve/output/stats'"/>
    <xsl:preserve-space elements="value"/>

    <xsl:template match="/">
        <xsl:text disable-output-escaping="yes"><![CDATA[
<!DOCTYPE dataset [
<!ELEMENT dataset (header,body)> <!ATTLIST dataset name CDATA #REQUIRED> <!ATTLIST dataset version CDATA "3.5.4"> <!ELEMENT header (notes?,attributes)> <!ELEMENT body (instances)> <!ELEMENT notes ANY> <!-- comments, information, copyright, etc. -->
<!ELEMENT attributes (attribute+)> <!ELEMENT attribute (labels?,metadata?,attributes?)> <!ATTLIST attribute name CDATA #REQUIRED> <!ATTLIST attribute type (numeric|date|nominal|string|relational) #REQUIRED> <!ATTLIST attribute format CDATA #IMPLIED>
<!ATTLIST attribute class (yes|no) "no"> <!ELEMENT labels (label*)> <!-- only for type "nominal" --> <!ELEMENT label ANY> <!ELEMENT metadata (property*)> <!ELEMENT property ANY> <!ATTLIST property name CDATA #REQUIRED> <!ELEMENT instances (instance*)>
<!ELEMENT instance (value*)> <!ATTLIST instance type (normal|sparse) "normal"> <!ATTLIST instance weight CDATA #IMPLIED> <!ELEMENT value (#PCDATA|instances)*> <!ATTLIST value index CDATA #IMPLIED> <!-- 1-based index (only used for instance format
"sparse") --> <!ATTLIST value missing (yes|no) "no"> ]>
  ]]></xsl:text>
        <dataset name="avve">
            <header>
                <attributes>
                    <xsl:for-each select="collection(concat($sourceFolder,'?select=*.xml;recurse=yes'))[1]/dataset/header/attributes/attribute[position() > 1 and position() ne last()]">
                        <attribute>
                            <xsl:attribute name="name" select="@name"/>
                            <xsl:attribute name="type" select="@type"/>
                        </attribute>
                    </xsl:for-each>
                    <attribute class="yes" name="class" type="nominal">
                        <labels>
                            <xsl:for-each select="distinct-values(collection(concat($sourceFolder,'?select=*.xml;recurse=yes'))/dataset/body/instances/instance/value[position() = last()])">
                                <xsl:sort select="."/>
                                <label><xsl:value-of select="normalize-space(.)"/></label>
                            </xsl:for-each>
                        </labels>
                    </attribute>
                </attributes>
            </header>
            <body>
                <instances>
                    <xsl:apply-templates mode="inFile" select="collection(concat($sourceFolder,'?select=*.xml;recurse=yes'))"/>
                </instances>
            </body>
        </dataset>
    </xsl:template>
    
    <xsl:template mode="inFile" match="*">
        <xsl:apply-templates select="*" mode="inFile"/>
    </xsl:template>
    
    <xsl:template mode="inFile" match="instance">
        <xsl:comment><xsl:value-of select="base-uri()"/></xsl:comment>
        <xsl:copy>
            <xsl:apply-templates select="value[position() > 1]" mode="inFile"/> <!--[position() &gt; 1]--> <!-- ignoring the large string value elements -->
        </xsl:copy>
    </xsl:template>
    
    <xsl:template mode="inFile" match="value">
        <xsl:copy>
            <xsl:apply-templates select="comment()|text()" mode="inFile"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template mode="inFile" match="comment()">
        <xsl:copy/>
    </xsl:template>
    
    <xsl:template mode="inFile" match="text()">
        <xsl:value-of select="."/>
    </xsl:template>
    
</xsl:stylesheet>
