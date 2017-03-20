import org.eclipse.emf.common.util.EMap
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.*
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl

import java.nio.file.Files

description "Generate domain classes from Ecore model (EMF)", "grails generate-domain-class example.ecore"

def modelName = args[0]
println "Generate domain class from Ecore model " + modelName

def modelFile = file("grails-app/model/" + modelName)
def domainPath = file("grails-app/domain/")

/**
 * Created by quentin on 19/03/17.
 */
class GenerateModel {

    private static final TAB_SPACE = "    ";
    private static final NEW_LINE = "\n";


    def static void generateModel(File vEcore, File vOutput) {

        // Create a resource set to hold the resources.
        ResourceSet resourceSet = new ResourceSetImpl();

        // Register the appropriate resource factory to handle all file extensions.
        Map<String, Object> options = resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap();
        options.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

        URL vEcoreURL = vEcore.toURL();
        String vURIAbsolute = vEcoreURL.toString().substring(vEcoreURL.getProtocol().length() + 1);
        URI uri = URI.createFileURI(vURIAbsolute);

        Resource resource = resourceSet.getResource(uri, true);
        println "Loaded " + uri;
        EPackage ePackage = (EPackage) resource.getContents().get(0);

        for (EClassifier eClassifier : ePackage.getEClassifiers()) {
            EClass eClass = (EClass) eClassifier;
            String content = generateFile(ePackage, eClass);
            File file = new File(vOutput, eClass.getName() + ".groovy");
            System.out.println("Generate file : " + file.getName());
            Files.write(file.toPath(), Collections.singletonList(content));
        }

    }

    private def static String generateFile(final EPackage ePackage, final EClass eClass) {

        final List<String> fields = new ArrayList();

        final StringBuilder content = new StringBuilder(1024);
        content.append("package ").append(ePackage.getName()).append(NEW_LINE);
        content.append(NEW_LINE);
        content.append("class ").append(eClass.getName()).append(" {");
        // Fields
        content.append(NEW_LINE).append(NEW_LINE);
        for (final EAttribute eAttribute : eClass.getEAllAttributes()) {
            content.append(TAB_SPACE).append(getType(eAttribute.getEType())).append(" ").append(eAttribute.getName()).append(NEW_LINE);
            fields.add(eAttribute.getName());
        }
        content.append(NEW_LINE);

        // belongsTo
        final List<EReference> belongsToRefs = getBelongsTo(eClass);
        if (!belongsToRefs.isEmpty()) {
            content.append(NEW_LINE).append(NEW_LINE).append(TAB_SPACE).append("static belongsTo = [");
            for (int i = 0; i < belongsToRefs.size(); i++) {
                if (i > 0) {
                    content.append(", ");
                }
                content.append(belongsToRefs.get(i).getName()).append(": ").append(getType(belongsToRefs.get(i).getEReferenceType()));
            }
            content.append("]").append(NEW_LINE).append(NEW_LINE);
        }

        // hasMany
        final List<EReference> hasManyRefs = getHasMany(eClass);
        if (!hasManyRefs.isEmpty()) {
            content.append(TAB_SPACE).append("static hasMany = [");
            for (int i = 0; i < hasManyRefs.size(); i++) {
                if (i > 0) {
                    content.append(", ");
                }
                content.append(hasManyRefs.get(i).getName()).append(": ").append(getType(hasManyRefs.get(i).getEReferenceType()));
            }
            content.append("]").append(NEW_LINE).append(NEW_LINE);
        }

        // toString method
        content.append(TAB_SPACE).append("String toString() {").append(NEW_LINE);
        content.append(TAB_SPACE).append(TAB_SPACE).append("return ");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                content.append(" + ' ' + ");
            }
            content.append("this.").append(fields.get(i));
        }
        content.append(NEW_LINE);
        content.append(TAB_SPACE).append("}").append(NEW_LINE).append(NEW_LINE);

        // static constraints
        content.append(TAB_SPACE).append("static constraints = {").append(NEW_LINE);
        for (final EAttribute eAttribute : eClass.getEAllAttributes()) {
            final Map<String, String> constraints = getConstraints(eAttribute);
            if (!constraints.isEmpty()) {
                content.append(TAB_SPACE).append(TAB_SPACE).append(eAttribute.getName()).append(' ');
                int i = 0;
                for (final Map.Entry<String, String> entry : constraints.entrySet()) {
                    if (i > 0)
                        content.append(", ");
                    content.append(entry.getKey()).append(": ").append(entry.getValue());
                    i++;
                }
                content.append(NEW_LINE);
            }
        }
        content.append(TAB_SPACE).append("}").append(NEW_LINE);

        content.append(NEW_LINE);
        content.append("}");
        return content.toString();
    }

    private def static String getType(EClassifier eType) {
        String rType = eType.getName();
        if (rType == "EString") {
            rType = "String";
        }
        rType;
    }

    private def static List getHasMany(EClass eClass) {
        List references = eClass.getEAllReferences().findAll { it.isContainment() }
        references
    }

    private def static List getBelongsTo(EClass eClass) {
        List references = eClass.getEAllReferences().findAll { !it.isContainment() }
        references;
    }

    private def static Map<String, String> getConstraints(final EAttribute eAttribute) {
        Map<String, String> rConstraints = new HashMap<>();
        EAnnotation eAnnotation = eAttribute.getEAnnotation("CONSTRAINTS");
        if (eAnnotation != null) {
            EMap<String, String> constraints = eAnnotation.getDetails();
            if (constraints != null) {
                for (final Map.Entry<String, String> entry : constraints.entrySet()) {
                    rConstraints.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return rConstraints;
    }
}

println "File found in \"grails-app/model/" + modelName + "\" : " + modelFile.exists()
println "Generate file in \"grails-app/domain/\""
GenerateModel.generateModel(modelFile, domainPath)
