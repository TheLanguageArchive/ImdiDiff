<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    exclude-result-prefixes="xs"
    xpath-default-namespace="http://www.mpi.nl/IMDI/Schema/IMDI"
    
    version="2.0">
    
    <xsl:output method="xml" indent="yes"/>
    <xsl:strip-space elements="*"/>
    
    <xsl:template match="node() | @*">
        <!-- base case: copy all child nodes and attributes recursively -->
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="METATRANSCRIPT/@ArchiveHandle[not(ends-with(., '@format=imdi'))]">
        <!-- add @format=imdi to self handle -->
        <xsl:attribute name="ArchiveHandle" select="concat(., '@format=imdi')" />
    </xsl:template>
    
    <xsl:template priority="10" match="/METATRANSCRIPT/@Originator | /METATRANSCRIPT/@Version | /METATRANSCRIPT/@xsi:schemaLocation">
        <!-- ignore some root node attributes -->
    </xsl:template>
    
    <xsl:template match="@Link | /METATRANSCRIPT/*//@Type">
        <!-- ignore vocab link and vocab type (not @Type on root node)-->
    </xsl:template>
    
    <xsl:template match="node()[normalize-space(.) = ''] | @*[normalize-space(.) = '']">
        <!-- ignore empty elements -->
    </xsl:template>
    
    <xsl:template match="@LanguageId[contains(.,':')]">
        <!-- remove language code scheme -->
        <xsl:attribute name="LanguageId" select="substring-after(.,':')" />
    </xsl:template>
    
    <xsl:template match="ResourceLink/text()">
        <!-- Remove everything up to last slash from resource link -->
        <xsl:value-of select="replace(.,'.*/','')" />
    </xsl:template>
    
</xsl:stylesheet>
