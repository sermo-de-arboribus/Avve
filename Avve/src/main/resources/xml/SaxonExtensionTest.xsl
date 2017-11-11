<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:weka="http://weka.sourceforge.net"
    exclude-result-prefixes="xs weka"
    version="2.0">
    <xsl:template match="/">
        <xsl:message>Classifying ...<xsl:value-of select="document-uri(/)"/></xsl:message>
        
        <!-- Call weka classifier and put result into a variable -->
        <xsl:variable name="classificationResult">
            <xsl:value-of select="weka:classify(document(document-uri(/)), 
                '/home/kai/git/Avve/Avve/weka/ergebnisse/model/naiveBayes__1_1_NaiveBayes.model')"/>
        </xsl:variable>
        <xsl:message>Document class is: <xsl:value-of select="$classificationResult"/></xsl:message>
        
        <!-- Write result to result document -->
        <result>
            <xsl:value-of select="$classificationResult"/>
        </result>
    </xsl:template>
</xsl:stylesheet>