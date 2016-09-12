<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns="http://www.mpi.nl/IMDI/Schema/IMDI"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:sil="http://www.sil.org/"
    xmlns:iso="http://www.iso.org/"
    exclude-result-prefixes="xs"
    xpath-default-namespace="http://www.mpi.nl/IMDI/Schema/IMDI"
    
    version="2.0">
    
    <xsl:output method="xml" indent="yes"/>
    <xsl:strip-space elements="*"/>
    
    <xsl:param name="sil-to-iso-url" select="'https://raw.githubusercontent.com/TheLanguageArchive/MetadataTranslator/development/Translator/src/main/resources/templates/imdi2cmdi/sil_to_iso6393.xml'" />
    <xsl:variable name="sil-lang-top" select="document($sil-to-iso-url)/sil:languages"/>
    <xsl:key name="sil-lookup" match="sil:lang" use="sil:sil"/>
    
    
    <xsl:variable name="iso-lang-uri" select="'https://raw.githubusercontent.com/TheLanguageArchive/MetadataTranslator/development/Translator/src/main/resources/templates/imdi2cmdi/iso2iso.xml'"/>
    <xsl:variable name="iso-lang-doc" select="document($iso-lang-uri)"/>
    <xsl:variable name="iso-lang-top" select="$iso-lang-doc/iso:m"/>
    <xsl:key name="iso639_2-lookup" match="iso:e" use="iso:b|iso:t"/>
    
    <xsl:template match="node() | @*">
        <!-- base case: copy all child nodes and attributes recursively -->
        <xsl:copy>
            <xsl:apply-templates select="@*" />
            
            <!-- Gather descriptions ... -->
            <xsl:variable name="descriptions">
             <xsl:apply-templates select="node()[name()='Description']" />             
             <xsl:if test="name() = 'Session'">
                 <!-- Copy info link descriptions from //Content to the appropriate place -->
                 <xsl:apply-templates select="." mode="copy-content-infolinks" />
             </xsl:if>
            </xsl:variable>
            
            <!-- Sort the combined descriptions -->
            <xsl:for-each select="$descriptions/Description">
                <!-- Sort descriptions by value -->
                <xsl:sort select="concat(normalize-space(replace(replace(.,'/$',''),'.*/','')),normalize-space(@ArchiveHandle),text())" />
                <xsl:copy-of select="." />
            </xsl:for-each>
            
            <xsl:if test="name() = 'Corpus'">
                <xsl:for-each-group select="CorpusLink" group-by="concat(@ArchiveHandle,@Name,text())">
                    <xsl:apply-templates select="." />
                </xsl:for-each-group>
            </xsl:if>
            
            <xsl:apply-templates select="node()[name()='Key']">
                <!-- Sort descriptions by value -->
                <xsl:sort select="concat(@Name, text())" />
            </xsl:apply-templates>
            <xsl:apply-templates select="node()[not(name()='Description' or name()='Key' or name()='CorpusLink')]" />
        </xsl:copy>
    </xsl:template>
        
    <xsl:template match="/METATRANSCRIPT/Session" mode="copy-content-infolinks">        
        <!-- Move InfoLinks in Content to Session level -->
        <xsl:for-each select="/METATRANSCRIPT/Session/MDGroup/Content/Description[normalize-space(@Link) != '']">
            <xsl:call-template name="copy-description" />
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template priority="60" match="/METATRANSCRIPT/Session/MDGroup/Content/Description[normalize-space(@Link) != '']">
        <!-- Skip InfoLinks in Content (they get moved to Session level, see template below ) -->
    </xsl:template>
    
    <xsl:template priority="50" match="Description">
        <xsl:call-template name="copy-description"/>
    </xsl:template>
    
    <xsl:template priority="60" match="Actor/Description|Languages/Description|Language/Description">
        <!-- only the content and language code of these descriptions should be kept --> 
        <xsl:if test="normalize-space(.) != ''">
            <Description>
                <xsl:if test="not(normalize-space(@LanguageId) = ('', 'Unspecified', 'Unknown'))">
                    <xsl:apply-templates select="@LanguageId"/>
                </xsl:if>
                <xsl:value-of select="." />
            </Description>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="copy-description">
        <xsl:if test="normalize-space(text()) != '' or normalize-space(@Link) != ''">
            <xsl:copy>
                <!-- Only keep language ID if there is text content -->
                <xsl:if test="not(normalize-space(@LanguageId) = ('', 'Unspecified', 'Unknown')) and normalize-space(text()) != ''">
                    <xsl:apply-templates select="@LanguageId"/>
                </xsl:if>
                <!-- keep @Link -->
                <xsl:if test="normalize-space(@Link) != ''">
                    <xsl:apply-templates select="@Link"/>
                </xsl:if>
                <!-- keep @ArchiveHandle -->
                <xsl:if test="normalize-space(@ArchiveHandle) != ''">
                    <xsl:apply-templates select="@ArchiveHandle"/>
                </xsl:if>
                <xsl:copy-of select="text()"/>
            </xsl:copy>
        </xsl:if>
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
    
    <xsl:template priority="30" match="/METATRANSCRIPT/@Originator | /METATRANSCRIPT/@Version | /METATRANSCRIPT/@xsi:schemaLocation | /METATRANSCRIPT/@FormatId | /METATRANSCRIPT/History | /METATRANSCRIPT/Corpus/@CorpusStructureService">
        <!-- ignore some root node attributes -->
    </xsl:template>    
    
    <xsl:template priority="20" match="Corpus/@CorpusStructureService | Corpus/@CatalogueHandle | Corpus/@CatalogueLink | Corpus/@SearchService">
        <!-- ignore some root node attributes -->
    </xsl:template>    

    <!-- Anonyms -->
    <xsl:template match="Anonyms">
        <!-- ignore anonyms (for now)-->
        <!-- TODO: Anonyms may need to be removed from original IMDIs, revisit! -->
    </xsl:template>    
    <xsl:template match="Resources[count(child::Anonyms) = count(child::*)]">
        <!-- skip Resource elements that only have Anonyms as children -->
    </xsl:template>
    
    <xsl:template match="@Link[not(parent::Description or parent::description)] | /METATRANSCRIPT/*//@Type">
        <!-- ignore vocab link and vocab type (not @Type on root node)-->
    </xsl:template>    
    
    <xsl:template match="@XXX-Type|@XXX-Multiple|@XXX-Tag|@XXX-Visible|@XXX-HelpText">
        <!-- Ignore these attributes (used in DBD and maybe other corpora) -->
    </xsl:template>
    
    <xsl:template match="@LanguageId[contains(.,':')]">
        <xsl:choose>
            <xsl:when test=". = 'ISO639:en'">
                <xsl:attribute name="LanguageId">eng</xsl:attribute>                
            </xsl:when>
            <xsl:when test=". = 'ISO639-2:ger'">
                <xsl:attribute name="LanguageId">deu</xsl:attribute>                
            </xsl:when>
            <xsl:when test=". = 'ISO639-2:dut'">
                <xsl:attribute name="LanguageId">nld</xsl:attribute>                
            </xsl:when>
            <xsl:otherwise>
                <!-- remove language code scheme -->
                <xsl:attribute name="LanguageId" select="substring-after(.,':')" />                
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="Language/Id" priority="20">
        <xsl:variable name="codeset" select="normalize-space(replace(substring-before(.,':'),' ',''))"/>
        <xsl:variable name="codestr" select="normalize-space(lower-case(substring-after(.,':')))"/>
        <Id>
        <xsl:choose>
            <xsl:when test="normalize-space(.) = ''">und</xsl:when>
            <xsl:when test="normalize-space(lower-case(.)) = 'unspecified'">und</xsl:when>
            <xsl:when test="normalize-space(lower-case(.)) = 'unknown'">und</xsl:when>
            <xsl:when test="normalize-space(lower-case(.)) = 'xxx'">und</xsl:when>
            <xsl:when test="$codeset='ISO639-2'">
                <xsl:choose>
                    <xsl:when test="$codestr='xxx'">
                        <xsl:value-of select="'und'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:variable name="iso" select="key('iso639_2-lookup', lower-case($codestr), $iso-lang-top)/iso:i"/>
                        <xsl:choose>
                            <xsl:when test="$iso!='xxx'">
                                <xsl:value-of select="$iso"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="'und'"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="$codeset='RFC1766'">
                <xsl:choose>
                    <xsl:when test="starts-with($codestr,'x-sil-')">
                        <xsl:variable name="iso" select="key('sil-lookup', lower-case(replace($codestr, 'x-sil-', '')), $sil-lang-top)/sil:iso"/>
                        <xsl:choose>
                            <xsl:when test="$iso!='xxx'">
                                <xsl:value-of select="$iso"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="'und'"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="'und'"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="$codestr='xxx'">
                        <xsl:value-of select="'und'"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$codestr" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
        </Id>
    </xsl:template>
    
    <xsl:template match="ResourceLink/text() | MediaResourceLink/text()">
        <xsl:variable name="mediaResourceLink">
            <xsl:for-each select="tokenize(.,' ')">
            <!-- Remove everything up to last slash from resource link -->
                <xsl:value-of select="replace(.,'.*/','')" />
                <xsl:text> </xsl:text>
            </xsl:for-each>
        </xsl:variable>
        <xsl:value-of select="normalize-space($mediaResourceLink)" />
    </xsl:template>
    
    <xsl:template match="@Link[parent::Description or parent::description]">
        <!-- Remove everything up to last slash from resource link -->
        <xsl:attribute name="Link" select="replace(replace(.,'/$',''),'.*/','')" />
    </xsl:template>    
    
    <xsl:template match="MediaResourceLink/@ArchiveHandle">
        <!-- Remove this attribute, it gets added in the CMDI2IMDI conversion -->
    </xsl:template>
    
    <xsl:template match="CorpusLink/text()">
        <!-- Remove everything up to last slash from resource link and remove translation service -->
        <xsl:value-of select="
            replace(
                replace(.,'.*(/|%2F)',''),
                '.cmdi&amp;outFormat=imdi', '.imdi')" />
    </xsl:template>
    
    <!-- Empty fields will be ignored except for a set of cases where empty gets mapped to unspecified -->
    
    <xsl:template priority="10"  match="node()[count(descendant::*[@Type = 'ClosedVocabulary']) = 0 and normalize-space(.) = ''] | @*[normalize-space(.) = '']">
        <!-- ignore empty elements without children -->
    </xsl:template>
    
    <xsl:template match="*[(name() = 'CommunicationContext' or name() = 'Access' or name() = 'Location' or name() = 'Validation') 
            and count(
                descendant::*[normalize-space(text()) != '' and normalize-space(text()) != 'Unspecified'])
            = 0]">
        <!-- ignore CommunicationContext or Access elements without value carrying children -->
        <!-- notice that we are ignoring attributes here, only looking at content! -->
    </xsl:template>
    
    <xsl:template match="Resources">
        <!-- sort resource elements -->
        <xsl:copy>
            <xsl:apply-templates>
                <xsl:sort select="concat(name(),ResourceLink/@ArchiveHandle,ResourceLink/text())"></xsl:sort>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
    
    <!-- ResourceId and ResourceRef -->
    
    <xsl:template match="MediaFile|WrittenResource" mode="make-resource-id">
        <!-- Makes a canonical ID substitute based on handle or resource name that should survive re-identification -->
        <xsl:choose>
            <xsl:when test="normalize-space(ResourceLink/@ArchiveHandle) != ''">
                <!-- handle exists, take it -->
                <xsl:value-of select="ResourceLink/@ArchiveHandle"/>
            </xsl:when>
            <xsl:otherwise>
                <!-- no handle, take file name without path -->
                <xsl:value-of select="replace(ResourceLink/text(),'.*/','')"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="WrittenResource/@ResourceId|MediaFile/@ResourceId">
        <xsl:if test="normalize-space(.) != '' and (//Actor[contains(@ResourceRef, current())]|//Language[contains(@ResourceRef, current())]|//Source[contains(@ResourceRefs,current())])">
            <!-- only keep resource id if there is a reference to it from a source or actor -->
            <xsl:variable name="id">
                <xsl:apply-templates select="parent::MediaFile|parent::WrittenResource" mode="make-resource-id" />
            </xsl:variable>
            <xsl:if test="normalize-space($id) != ''">
                <xsl:attribute name="ResourceId" select="$id" />
            </xsl:if>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="Actor/@ResourceRef|Language/@ResourceRef">
        <xsl:if test="normalize-space(.) != ''">
            <xsl:variable name="mf" select="//MediaFile" />
            <xsl:variable name="wr" select="//WrittenResource" />
            <xsl:variable name="resourceRef">
                <xsl:for-each select="tokenize(., '\s')">
                    <xsl:variable name="resource" select="$mf[@ResourceId=current()]|$wr[@ResourceId=current()]"/>
                    <xsl:if test="$resource">
                        <xsl:apply-templates select="$resource" mode="make-resource-id" />
                        <xsl:text> </xsl:text>
                    </xsl:if>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="normalize-space($resourceRef) != ''">
                <xsl:attribute name="ResourceRef">
                    <xsl:value-of select="normalize-space($resourceRef)" />
                </xsl:attribute>
            </xsl:if>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="Source/@ResourceRefs">
        <xsl:variable name="root" select="/" />
        
        <!-- only keep refs that resolve to a resource -->
        <xsl:variable name="refs">
            <!-- there may be multiple refs, so tokenize -->
            <xsl:for-each select="tokenize(.,'\s')">
                <!-- look for a matching resource... --> 
                <xsl:variable name="resource" select="$root//MediaFile[@ResourceId=current()]|$root//WrittenResource[@ResourceId=current()]"/>
                <xsl:if test="$resource">
                    <!-- use a canonical id (same as used as substitute in resource) -->
                    <xsl:variable name="resourceName">
                        <xsl:apply-templates select="$resource" mode="make-resource-id" />
                    </xsl:variable>
                    <!-- and append some whitespace for separation -->
                    <xsl:value-of select="concat($resourceName,' ')" />
                </xsl:if>
                <!-- 'dangling' refs just get skipped ... -->
            </xsl:for-each>
        </xsl:variable>
        
        <!-- only insert attribute if there are resolving refs.. -->
        <xsl:if test="normalize-space($refs) != ''">
            <xsl:attribute name="ResourceRefs" select="normalize-space($refs)" />
        </xsl:if>
    </xsl:template>   
    
    <xsl:template match="Age" priority="20">
        <xsl:if test="normalize-space(.) != 'Unspecified' and normalize-space(.) != ''">
            <Age>
                <!-- age example: 4;06.00 -->
                <!-- range example: 22;6/22;7 -->
                <xsl:analyze-string 
                    select="." 
                    regex="^([0-9]{{1,3}})(;(0?[0-9]|1[01])(\.(0?[0-9]|[12][0-9]|30))?)?/([0-9]{{1,3}})(;(0?[0-9]|1[01])(\.(0?[0-9]|[12][0-9]|30))?)?$">
                    <xsl:matching-substring>
                        <!--age range -->
                        <xsl:value-of select="number(regex-group(1))"/>;<xsl:value-of select="number(regex-group(3))"/>.<xsl:value-of select="number(regex-group(5))"/><xsl:value-of select="'/'"/>
                        <xsl:value-of select="number(regex-group(6))"/>;<xsl:value-of select="number(regex-group(8))"/>.<xsl:value-of select="number(regex-group(10))"/>
                    </xsl:matching-substring>
                    <xsl:non-matching-substring>
                        <xsl:analyze-string 
                            select="." 
                            regex="^([0-9]{{1,3}})(;(0?[0-9]|1[01])(\.(0?[0-9]|[12][0-9]|30))?)?$">
                            <xsl:matching-substring>
                                <!--age range -->
                                <xsl:value-of select="number(regex-group(1))"/>;<xsl:value-of select="number(regex-group(3))"/>.<xsl:value-of select="number(regex-group(5))"/>
                            </xsl:matching-substring>
                            <xsl:non-matching-substring>
                                <!-- Neither exact age or age range -->
                                <xsl:value-of select="." />
                            </xsl:non-matching-substring>
                        </xsl:analyze-string>
                    </xsl:non-matching-substring>
                </xsl:analyze-string>
            </Age>
        </xsl:if>
    </xsl:template>
    
    <!-- Exceptional cases where an empty value should get normalised to 'unspecified' (these are generally of the closed vocabulary type) -->     
    <xsl:template match="
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
        |Validation/Level
        |Access/Date
        |WrittenResource/Date
        |Actor/Age
        |Actor/BirthDate
        |CounterPosition/End
        |TimePosition/Start
        |TimePosition/End
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
        <!-- Remove element if value is Unspecified -->
        <xsl:if test="normalize-space(.) != 'Unspecified'">
            <xsl:copy>
                <xsl:apply-templates select="@* | node()" />
            </xsl:copy>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="Content/Modalities">
        <!-- For DBD, which does not preserve the lower case values -->
        <xsl:copy><xsl:value-of select="lower-case(.)" /></xsl:copy> 
    </xsl:template>
</xsl:stylesheet>
