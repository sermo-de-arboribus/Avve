<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output encoding="UTF-8" indent="yes"/>
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
        <xsl:apply-templates />
    </xsl:template>
    
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="attribute[not(@name = 'class')]">
        <xsl:copy>
            <xsl:apply-templates select="*|@*|text()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="attribute[@name = 'class']">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <labels>
                <xsl:for-each select="distinct-values(labels/label/(substring(.,1, 2)))">
                    <label><xsl:value-of select="."/></label>
                </xsl:for-each>
            </labels>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="value">
        <xsl:copy>
            <xsl:apply-templates select="comment()|text()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="value[position() = last()]">
        <value><xsl:comment>class</xsl:comment><xsl:value-of select="substring(normalize-space(.), 1, 2)"/></value>
    </xsl:template>
    
    <xsl:template match="comment()|@*">
        <xsl:copy/>
    </xsl:template>
    
    <xsl:template match="text()">
        <xsl:value-of select="."/>
    </xsl:template>
    
</xsl:stylesheet>
