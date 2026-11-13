/* (c) https://github.com/MontiCore/monticore */
package umlp.generator.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.monticore.cd4codebasis._symboltable.CDMethodSignatureSymbol;
import de.monticore.cdbasis._ast.ASTCDClass;
import de.monticore.symboltable.ISymbol;
import de.monticore.symbols.basicsymbols._symboltable.TypeSymbol;
import de.monticore.symbols.basicsymbols._symboltable.VariableSymbol;
import de.monticore.tagging.SimpleSymbolTagger;
import de.monticore.tagging.TagRepository;
import de.monticore.tagging.tags._ast.ASTSimpleTag;
import de.monticore.tagging.tags._ast.ASTTag;
import de.se_rwth.commons.logging.Log;
import org.jetbrains.annotations.NotNull;

public class TagLookUp {

  // TODO: Turn this into a real singleton?

  private final static String SINGLETON = "singleton";
  private final static String COMMAND = "command";
  private final static String DERIVED = "derived";
  private final static String POJO = "pojo";
  // target role of an attribute qualified association (a GemMap), see TagRelated.AttributeQualifiedRoleTagVisitor
  public final static String ATTRIBUTE_QUALIFIED = "attributeQualified";
  private final static String DOMAIN_VALIDATION_ENTRY = "domainValidationEntry";
  private final static String DOMAIN_VALIDATION_TARGET = "domainValidationTarget";

  protected static SimpleSymbolTagger _simpleSymbolTagger;

  protected static SimpleSymbolTagger getSimpleSymbolTagger() {
    if (_simpleSymbolTagger == null) {
      _simpleSymbolTagger = new UMLPSimpleSymbolTagger(TagRepository::getLoadedTagUnits);
    }
    return _simpleSymbolTagger;
  }

  // Consider caching this within the taggers?
  protected static LoadingCache<TypeSymbol, Boolean> isSingletonCacheSymbol = CacheBuilder.newBuilder()
      .build(new CacheLoader<>() {
        @Override
        public @NotNull Boolean load(@NotNull TypeSymbol key) {
          return _isSingleton(key);
        }
      });

  protected static LoadingCache<CDMethodSignatureSymbol, Boolean> isCommandCacheSymbol = CacheBuilder.newBuilder()
      .build(new CacheLoader<>() {
        @Override
        public @NotNull Boolean load(@NotNull CDMethodSignatureSymbol key) {
          return _isCommand(key);
        }
      });

  protected static LoadingCache<TypeSymbol, Boolean> isDerivedCacheSymbol = CacheBuilder.newBuilder()
      .build(new CacheLoader<>() {
        @Override
        public @NotNull Boolean load(@NotNull TypeSymbol key) {
          return _isDerived(key);
        }
      });

  protected static LoadingCache<VariableSymbol, Boolean> isDerivedAttrCacheSymbol = CacheBuilder.newBuilder()
      .build(new CacheLoader<>() {
        @Override
        public @NotNull Boolean load(@NotNull VariableSymbol key) {
          return _isDerived(key);
        }
      });

  protected static LoadingCache<TypeSymbol, Boolean> isPojoCacheSymbol = CacheBuilder.newBuilder()
      .build(new CacheLoader<>() {
        @Override
        public @NotNull Boolean load(@NotNull TypeSymbol key) {
          return _isPojo(key);
        }
      });

  public static boolean isSingleton(TypeSymbol typeSymbol) {
    return isSingletonCacheSymbol.getUnchecked(typeSymbol);
  }

  public static boolean isSingleton(ASTCDClass astcdClass) {
    boolean stereo = astcdClass.getModifier().isPresentStereotype() && astcdClass.getModifier().getStereotype().contains(SINGLETON);
    boolean tag = isSingleton(astcdClass.getSymbol());
    if (stereo != tag) {
      // Ensure tagging does work like it should
      // Only here for testing purposes
      Log.warn("Missing tag for " + astcdClass.getName(), astcdClass.get_SourcePositionStart());
    }
    return tag || stereo;
  }

  public static boolean isCommand(CDMethodSignatureSymbol methodSignatureSymbol) {
    return isCommandCacheSymbol.getUnchecked(methodSignatureSymbol);
  }

  public static boolean isDerived(TypeSymbol typeSymbol) {
    return isDerivedCacheSymbol.getUnchecked(typeSymbol);
  }

  public static boolean isDerived(VariableSymbol fieldSymbol) {
    return isDerivedAttrCacheSymbol.getUnchecked(fieldSymbol);
  }

  public static boolean isPojo(TypeSymbol typeSymbol) {
    return isPojoCacheSymbol.getUnchecked(typeSymbol);
  }

  /**
   * @return whether the role is the target of an attribute qualified association, i.e. a GemMap
   */
  public static boolean isAttributeQualified(ISymbol roleSymbol) {
    return hasSimpleTag(roleSymbol, ATTRIBUTE_QUALIFIED);
  }

  public static boolean isDomainValidationEntry(ISymbol symbol) {
    return hasSimpleTag(symbol, DOMAIN_VALIDATION_ENTRY);
  }

  public static boolean isDomainValidationTarget(ISymbol symbol) {
    return hasSimpleTag(symbol, DOMAIN_VALIDATION_TARGET);
  }

  protected static Boolean _isSingleton(TypeSymbol typeSymbol) {
    return getSimpleSymbolTagger().getTags(typeSymbol).stream().anyMatch(TagLookUp::isSingleton);
  }

  protected static boolean _isCommand(CDMethodSignatureSymbol symbol) {
    return getSimpleSymbolTagger().getTags(symbol).stream().anyMatch(TagLookUp::isCommand);
  }

  public static boolean _isDerived(TypeSymbol typeSymbol) {
    return getSimpleSymbolTagger().getTags(typeSymbol).stream().anyMatch(TagLookUp::isDerived);
  }

  public static boolean _isDerived(VariableSymbol typeSymbol) {
    return getSimpleSymbolTagger().getTags(typeSymbol).stream().anyMatch(TagLookUp::isDerived);
  }

  public static boolean _isPojo(TypeSymbol typeSymbol) {
    return getSimpleSymbolTagger().getTags(typeSymbol).stream().anyMatch(TagLookUp::isPojo);
  }

  protected static boolean hasSimpleTag(ISymbol symbol, String name) {
    return getSimpleSymbolTagger().getTags(symbol).stream()
        .anyMatch(tag -> isSimpleTag(tag, name));
  }

  // This would be nice to have generated from a TagSchema
  protected static boolean isSingleton(ASTTag tag){
    if (tag instanceof ASTSimpleTag)
      return ((ASTSimpleTag) tag).getName().equals(SINGLETON);
    return false;
  }

  protected static boolean isCommand(ASTTag tag){
    if (tag instanceof ASTSimpleTag)
      return ((ASTSimpleTag) tag).getName().equals(COMMAND);
    return false;
  }

  protected static boolean isDerived(ASTTag tag){
    if (tag instanceof ASTSimpleTag)
      return ((ASTSimpleTag) tag).getName().equals(DERIVED);
    return false;
  }

  protected static boolean isPojo(ASTTag tag){
    return isSimpleTag(tag, POJO);
  }

  protected static boolean isSimpleTag(ASTTag tag, String name){
    if (tag instanceof ASTSimpleTag)
      return ((ASTSimpleTag) tag).getName().equals(name);
    return false;
  }

  public static void cleanUp() {
    _simpleSymbolTagger = null;
    isSingletonCacheSymbol.invalidateAll();
    isSingletonCacheSymbol.cleanUp();
    isCommandCacheSymbol.invalidateAll();
    isCommandCacheSymbol.cleanUp();
    isDerivedCacheSymbol.invalidateAll();
    isDerivedCacheSymbol.cleanUp();
    isDerivedAttrCacheSymbol.invalidateAll();
    isDerivedAttrCacheSymbol.cleanUp();
    isPojoCacheSymbol.invalidateAll();
    isPojoCacheSymbol.cleanUp();
    TagRepository.clearTags();
  }
}
