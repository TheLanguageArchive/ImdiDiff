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
    
    <xsl:template match="METATRANSCRIPT/@Type[normalize-space(.) = 'CORPUS.Profile']">
        <xsl:attribute name="Type">CORPUS</xsl:attribute>
    </xsl:template>
    
    <xsl:template match="METATRANSCRIPT/@Type[normalize-space(.) = 'SESSION.Profile']">
        <xsl:attribute name="Type">SESSION</xsl:attribute>
    </xsl:template>
    
    <xsl:template match="METATRANSCRIPT/@ArchiveHandle[not(ends-with(., '@format=imdi'))]|CorpusLink/@ArchiveHandle[not(ends-with(., '@format=imdi'))]">
        <!-- add @format=imdi to self handle -->
        <xsl:attribute name="ArchiveHandle" select="concat(., '@format=imdi')" />
    </xsl:template>
    
    <xsl:template priority="10" match="/METATRANSCRIPT/@Originator | /METATRANSCRIPT/@Version | /METATRANSCRIPT/@xsi:schemaLocation | /METATRANSCRIPT/@FormatId | /METATRANSCRIPT/History">
        <!-- ignore some root node attributes -->
    </xsl:template>
    
    <xsl:template match="@Link | /METATRANSCRIPT/*//@Type">
        <!-- ignore vocab link and vocab type (not @Type on root node)-->
    </xsl:template>    
    
    <xsl:template match="@ResourceRef|@ResourceId">
        <!-- Resource references and id's will not be similar, so ignore -->
    </xsl:template>
    
    <xsl:template match="@XXX-Type|@XXX-Multiple">
        <!-- Ignore these attributes (used in DBD and maybe other corpora) -->
    </xsl:template>
    
    <xsl:template match="@LanguageId[contains(.,':')]">
        <!-- remove language code scheme -->
        <xsl:attribute name="LanguageId" select="substring-after(.,':')" />
    </xsl:template>
    
    <xsl:template match="ResourceLink/text()">
        <!-- Remove everything up to last slash from resource link -->
        <xsl:value-of select="replace(.,'.*/','')" />
    </xsl:template>
    
    <xsl:template match="CorpusLink/text()">
        <!-- Remove everything up to last slash from resource link and remove translation service -->
        <xsl:value-of select="
            replace(
                replace(.,'.*(/|%2F)',''),
                '.cmdi&amp;outFormat=imdi', '.imdi')" />
    </xsl:template>
    
    <!-- Empty fields will be ignored except for a set of cases where empty gets mapped to unspecified -->
    
    <xsl:template match="node()[count(descendant::*[@Type = 'ClosedVocabulary']) = 0 and normalize-space(.) = ''] | @*[normalize-space(.) = '']">
        <!-- ignore empty elements without children -->
    </xsl:template>
    
    <!-- Exceptional cases where an empty value should get normalised to 'unspecified' (these are generally of the closed vocabulary type) -->     
    <xsl:template priority="10" match="
        //Anonymized	
        |Channel	
        |Continent	
        |Country	
        |Derivation	
        |Discursive 	
        |Dominant	
        |EventStructure	
        |Genre	
        |Interactivity	
        |Involvement	
        |LanguageId
        |Methodology	
        |MotherTongue	
        |Language/Name
        |PlanningType	
        |PrimaryLanguage	
        |Quality	
        |Role	
        |Sex	
        |SocialContext	
        |SourceLanguage
        |SubGenre	
        |TargetLanguage
        |Task	
        |Role	
        |MediaFile/Type
        |Validation/Type
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.communitySize.xml' and @Name='CGN.education.placesize']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.EducationLevel.xml' and @Name='CGN.education.level']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Language.xml' and @Name='CGN.firstLang']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Language.xml' and @Name='CGN.homeLang']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Language.xml' and @Name='CGN.workLang']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Locale.xml' and @Name='CGN.locale']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.OccupationLevel.xml' and @Name='CGN.occupation.level']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.recDate.xml' and @Name='CGN.recDate']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Region.xml' and @Name='CGN.education.reg']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Region.xml' and @Name='CGN.residence.reg']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.segmentation.xml' and @Name='CGN.segmentation']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/Content-EventStructure.xml' and @Name='Content-EventStructure']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/Content-Genre.xml' and @Name='Content-Genre']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/Content-Interactivity.xml' and @Name='Content-Interactivity']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/Content-Involvement.xml' and @Name='Content-Involvement']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/Content-Modalities.xml' and @Name='Content-Modalities']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/Content-SocialContext.xml' and @Name='Content-SocialContext']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/Content-SubGenre-Discourse.xml' and @Name='Content-SubGenre-Discourse']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/Content-SubGenre-Singing.xml' and @Name='Content-SubGenre-Singing']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/Countries.xml' and @Name='Countries']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/Countries.xml' and @Name='DBD.CountryofBirth']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/DBD.AgeAtImmigration.xml' and @Name='DBD.AgeAtImmigration']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/IWSong-DidjeriduStyle.xml' and @Name='IWSong-DidjeriduStyle']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/IWSongNames-Jurtbirrk.xml' and @Name='IWSongNames-Jurtbirrk']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/IWSongNames-Kalajbari.xml' and @Name='IWSongNames-Kalajbari']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/IWSong-Tempo.xml' and @Name='IWSong-Tempo']
        |Key[@Link='http://www.mpi.nl/IMDI/Schema/IWSongTypes.xml' and @Name='IWSongTypes']
        |Key[@Link='http://www.zipworld.com.au/~lbarwick/IWSongNames-Jalarrkuku.xml' and @Name='IWSongNames-Jalarrkuku']
        |Key[@Link='http://www.zipworld.com.au/~lbarwick/IWSongNames-Jurtbirrk.xml' and @Name='IWSongNames-Jurtbirrk']
        |Key[@Link='http://www.zipworld.com.au/~lbarwick/IWSongNames-Kalajbari.xml' and @Name='IWSongNames-Kalajbari']
        |Key[@Link='http://www.zipworld.com.au/~lbarwick/IWSongNames-Marrwakani.xml' and @Name='IWSongNames-Marrwakani']
        |Key[@Link='http://www.zipworld.com.au/~lbarwick/IWSongNames-Mirrijbu.xml' and @Name='IWSongNames-Mirrijbu']
        |Key[@Link='http://www.zipworld.com.au/~lbarwick/IWSongNames-Yanajanak.xml' and @Name='IWSongNames-Yanajanak']
        |Key[@Link='http://www.zipworld.com.au/~lbarwick/IWSongNames-Yiwarruj.xml' and @Name='IWSongNames-Yiwarruj']
        |Key[@Link='http://www.zipworld.com.au/~lbarwick/IWSong-Tempo.xml' and @Name='IWSong-Tempo']
        |Key[@Link='http://www.zipworld.com.au/~lbarwick/IWSongTypes.xml' and @Name='IWSongTypes']
        |Key[@Link='http://sign-lang.ruhosting.nl/IMDI/vocabs/Deafness.AidType.xml' and @Name='Deafness.AidType']
        |Key[@Link='http://sign-lang.ruhosting.nl/IMDI/vocabs/Deafness.Status.xml' and @Name='Deafness.Status']
        |Key[@Link='http://sign-lang.ruhosting.nl/IMDI/vocabs/Deafness.Status.xml' and @Name='Family.Father.Deafness']
        |Key[@Link='http://sign-lang.ruhosting.nl/IMDI/vocabs/Deafness.Status.xml' and @Name='Family.Mother.Deafness']
        |Key[@Link='http://sign-lang.ruhosting.nl/IMDI/vocabs/Deafness.Status.xml' and @Name='Family.Partner.Deafness']
        |Key[@Link='http://sign-lang.ruhosting.nl/IMDI/vocabs/Interpreting.Audience.xml' and @Name='Interpreting.Audience']
        |Key[@Link='http://sign-lang.ruhosting.nl/IMDI/vocabs/Interpreting.Visibility.xml' and @Name='Interpreting.Visibility']
        |Key[@Link='http://www.zipworld.com.au/~lbarwick/IWSongTypes.xml' and @Name='IWSongTypes']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.EducationLevel.xml' and @Name='CGN.education.level']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.communitySize.xml' and @Name='CGN.education.placesize']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Region.xml' and @Name='CGN.education.reg']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Language.xml' and @Name='CGN.firstLang']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Language.xml' and @Name='CGN.homeLang']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Locale.xml' and @Name='CGN.locale']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.OccupationLevel.xml' and @Name='CGN.occupation.level']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.recDate.xml' and @Name='CGN.recDate']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Region.xml' and @Name='CGN.residence.reg']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.segmentation.xml' and @Name='CGN.segmentation']
        |Key[@Link='http://www.mpi.nl/CGN/Schema/CGN.Language.xml' and @Name='CGN.workLang']
        |Key[@Name='Deafness.AidType']
        |Key[@Name='Deafness.Status']
        |Key[@Name='Family.Father.Deafness']
        |Key[@Name='Family.Mother.Deafness']
        |Key[@Name='Family.Partner.Deafness']
        |Key[@Name='Family.Partner.Deafness']
        |Key[@Name='Interpreting.Audience']
        |Key[@Name='Interpreting.Audience']
        |Key[@Name='Interpreting.Visibility']
        |Key[@Name='Interpreting.Visibility']
        ">
        <xsl:element name="{name(.)}" namespace="{namespace-uri(.)}">
            <xsl:choose>
                <xsl:when test="normalize-space(.) = '' ">
                    <!-- Add 'unspecified' value for empty nodes with closed vocab -->
                    <xsl:text>Unspecified</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>
    
</xsl:stylesheet>
