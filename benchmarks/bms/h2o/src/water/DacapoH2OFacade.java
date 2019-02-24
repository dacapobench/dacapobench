package water;

import sun.security.provider.MD5;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.*;

public class DacapoH2OFacade {


    private RestUtil h2o_rest;

    private final String[] PARSE_PARAMETERS_NAME = new String[] {
            "destination_frame",
            "parse_type",
            "separator",
            "number_columns",
            "single_quotes",
            "source_frames",
            "column_names",
            "column_types",
            "check_header",
            "chunk_size",
            "delete_on_done"
    };
    private final String[] PARSE_SETUP_PARAMETERS_NAME = new String[] {
            "destination_frame",
            "parse_type",
            "separator",
            "number_columns",
            "single_quotes",
            "column_names",
            "column_types",
            "check_header",
            "chunk_size",
    };
    private final List<String> POSSIBLE_MODELS= Arrays.asList(
            "coxph",
            "deeplearning",
            "deepwater",
            "drf",
            "gbm",
            "glm",
            "glrm",
            "isolationforest",
            "kmeans",
            "naivebayes",
            "pca",
            "word2vec",
            "xgboost"
    );

    private final String DEFAULT_DRF_PARAMETER = "ignore_const_cols=true&ntrees=50&max_depth=20&min_rows=1&nbins=20&seed=-1&mtries=-1&sample_rate=0.6320000290870667&score_each_iteration=false&score_tree_interval=0&fold_assignment=AUTO&balance_classes=false&max_confusion_matrix_size=20&max_hit_ratio_k=0&nbins_top_level=1024&nbins_cats=1024&r2_stopping=1.7976931348623157e%2B308&stopping_rounds=0&stopping_metric=AUTO&stopping_tolerance=0.001&max_runtime_secs=0&checkpoint=&col_sample_rate_per_tree=1&min_split_improvement=0.00001&histogram_type=AUTO&categorical_encoding=AUTO&distribution=AUTO&custom_metric_func=&export_checkpoints_dir=&keep_cross_validation_models=true&keep_cross_validation_predictions=false&keep_cross_validation_fold_assignment=false&class_sampling_factors=&max_after_balance_size=5&build_tree_one_node=false&sample_rate_per_class=&binomial_double_trees=false&col_sample_rate_change_per_level=1&calibrate_model=false";
    private final String MODEL_MACRO = "${model}";
    // APIs

    // Typeahead hander for filename completion.
    private final String TYPE_HEAD= "3/Typeahead/files"; // http://docs.h2o.ai/h2o/latest-stable/h2o-docs/rest-api-reference.html#schema-TypeaheadV3
    // Typeahead hander for filename completion.
    private final String IMPORT_FILE= "3/ImportFiles"; // http://docs.h2o.ai/h2o/latest-stable/h2o-docs/rest-api-reference.html#route-%2F3%2FImportFiles
    // Guess the parameters for parsing raw byte-oriented data into an H2O Frame.
    private final String PARSE_SETUP= "3/ParseSetup"; //http://docs.h2o.ai/h2o/latest-stable/h2o-docs/rest-api-reference.html#route-%2F3%2FParseSetup
    // Parse a raw byte-oriented Frame into a useful columnar data Frame.
    private final String PARSE = "3/Parse"; //http://docs.h2o.ai/h2o/latest-stable/h2o-docs/rest-api-reference.html#route-%2F3%2FParse
    //Train a model.
    private final String MODEL_TRAIN = "3/ModelBuilders/" + MODEL_MACRO;
    //Validate a set of PCA model builder parameters.
    private final String MODEL_PARAMETERS_VALIDATE = "3/ModelBuilders/" + MODEL_MACRO + "/parameters";


    //
    private DacapoH2OFacade(String h2O_URL) {
        h2o_rest = new RestUtil(h2O_URL);
    }

    /**
     * Given a incomplete path, calling the RESTFul API of H2O. All possible path will be returned.
     *
     * @param path incomplete path to the target file
     * @return the possible completed path
     */
    private List getCompletedPath(String path){
        try {
            //todo: validate the attributes
            h2o_rest.getMethod(TYPE_HEAD + "?src=" + path);
        } catch (IOException e) {
            System.err.println("Failed to find the path: " + path);
        }
        return h2o_rest.getListFromResponse("matches").get("matches");
    }

    /**
     * Import the a data set into H2O. It could be a csv file or SQL table. A complete path should be offered.
     * For incomplete path, please refer to importFilesIncomplete(String incompletePath)
     *
     * @param path a complete path
     * @return a boolean indicate if importing succeed or not
     */
    private boolean importFiles(String path){
        try {
            h2o_rest.getMethod(IMPORT_FILE + "?path=" + path);
        } catch (IOException e) {
            System.err.println("Failed to import the file: " + path);
        }
        return h2o_rest.getFromResponse("fails").get("fail") == null;
    }

    /**
     * Offering an incomplete path, import possible data set into H2O. It could be a csv file.
     * For complete path, please refer to importFiles(String path)
     *
     * @param incompletePath a complete path
     * @return a boolean indicate if importing succeed or not
     */
    private List<String> importFilesIncomplete(String incompletePath){
        List<String> possiblePath = getCompletedPath(incompletePath);
        List<String> succeed = new LinkedList<>();
        System.out.println(possiblePath);
        try {
            for (String path : possiblePath) {
                if (path.endsWith("csv")) {
                    h2o_rest.getMethod(IMPORT_FILE + "?path=" + path);
                    succeed.add(path);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to import: " + incompletePath + "\n the possible path is:" + possiblePath);
        }
        return succeed;
    }

    /**
     *
     * @param sourceFrames the list of string, path to all sourceFrames
     * @return
     */
    private Map<String, String> getParseSetup(String... sourceFrames){
        // Convert into JSON format string
        StringBuilder para = new StringBuilder();
        for (String sourceFrame : sourceFrames) {
            para.append("source_frames=").append(sourceFrame);
        }
        try {
            h2o_rest.postMethod(PARSE_SETUP, para.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return h2o_rest.getFromResponse(PARSE_SETUP_PARAMETERS_NAME);
    }

    /**
     * @param parameters The POST method parameters, it should be the form data in the browser.
     */
    private void parse(Map<String, String> parameters){
        StringBuilder para = new StringBuilder();
        for (String name : PARSE_PARAMETERS_NAME) {
            try {
                para.append(name).append("=").append(parameters.get(name)).append("&");
            }catch (Exception e){
                System.err.println("Failed to get parameter: " + name);
            }
        }
        // Parsing the data frame to hex. A h2o designed data format.
        try {
            h2o_rest.postMethod(PARSE, para.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse the source file into a hex file
     *
     * @param sourceFormat
     */
    private void parse(String sourceFormat){
        if (!sourceFormat.startsWith("nfs:/"))
            sourceFormat = "nfs:/" + sourceFormat;
        Map<String, String> kv = getParseSetup(sourceFormat);
        kv.put("source_frames", sourceFormat);
        kv.put("delete_on_done", "true");
        parse(kv);
    }

    /**
     * Get the API to train a model
     *
     * @param model the name of a model
     * @return API
     */
    private String getModelAPI(String model){
        return checkModel(model);
    }

    /**
     * Get the API to validate the parameter for training a model
     *
     * @param model the name of a model
     * @return API
     */
    private String getModelValidateAPI(String model){
        return checkValidateModel(model);
    }

    /**
     * Check if this model is currently able to be used
     *
     * @param model The name of the model
     * @return the API
     */
    private String checkValidateModel(String model) {
        if (POSSIBLE_MODELS.contains(model))
            return MODEL_PARAMETERS_VALIDATE.replace(MODEL_MACRO, model);
        else
            throw new UnsupportedOperationException("No such model: " + model);
    }

    /**
     * Check if the model is currently able to be used
     *
     * @param model The name of the model
     * @return the API
     */
    private String checkModel(String model) {
        if (POSSIBLE_MODELS.contains(model))
            return MODEL_TRAIN.replace(MODEL_MACRO, model);
        else
            throw new UnsupportedOperationException("No such model: " + model);
    }

    /**
     * @return all models' name, some of them may be unavailable
     */
    public List<String> getPossibleModels() {
        return POSSIBLE_MODELS;
    }

    /**
     * @param para parameters for training the model
     * @return if the parameter is valid
     */
    private int check_drf_parameter(String para){
        String API = getModelValidateAPI("drf");
        try {
            h2o_rest.postMethod(API, para);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Integer.parseInt(h2o_rest.getFromResponse("error_count").get("error_count"));
    }

    /**
     * @param para parameters for training the model
     */
    public void train_drf(String para){
        try{
            int v = check_drf_parameter(para);
            if (v == 0){
                String API = getModelAPI("drf");
                h2o_rest.postMethod(API, para);
            } else {
                throw new InvalidParameterException("the parameters are invalid, there are " + v + " errors");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param train_frame the file to be used for training the drf model
     * @param response_column response variable column
     */
    public void train_drf(String train_frame, String response_column){
        String para = DEFAULT_DRF_PARAMETER + "&training_frame=" + train_frame + "&" + "response_column=" + response_column;

        para = para + "&model_id=" + "drf-dacapo-" + train_frame;
        train_drf(para);
    }
}
