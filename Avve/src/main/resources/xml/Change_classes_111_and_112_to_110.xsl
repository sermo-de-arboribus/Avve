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
    
    <xsl:template match="*[local-name() = 'class']">
        <xsl:choose>
            <xsl:when test=". = '110'"/>
            <xsl:when test=". = '111'"/>
            <xsl:when test=". = '112'"><xsl:copy>110</xsl:copy></xsl:when>
            <xsl:otherwise><xsl:apply-templates select="*|@*|text()"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="value">
        <xsl:copy>
            <xsl:apply-templates select="comment()|text()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="value[position() = last()]">
        <xsl:choose>
            <xsl:when test="normalize-space(.) = '111'"><value><xsl:comment>class</xsl:comment>110</value></xsl:when>
            <xsl:when test="normalize-space(.) = '112'"><value><xsl:comment>class</xsl:comment>110</value></xsl:when>
            <xsl:otherwise><value><xsl:comment>class</xsl:comment><xsl:value-of select="normalize-space(.)"/></value></xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>
    
    <xsl:template match="comment()|@*">
        <xsl:copy/>
    </xsl:template>
    
    <xsl:template match="text()">
        <xsl:value-of select="."/>
    </xsl:template>
    
</xsl:stylesheet>
