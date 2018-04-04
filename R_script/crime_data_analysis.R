# This script is to apply Deep Learning Autoencoders model on Los Angeles Crime Data to predict the arrest_status

# This function is to load the crime data which is got from socrata API.
fn.loadCrimeData <- function() {
	print("############## Importing Crime Data ###############")
	
	crime_data <- read.csv("socrata_crime_data.csv")
	return(crime_data)
}

# Function used to find the number of NA values in each column.
# Visualizes the results using ggplot.
fn.displayTotalNAcount <- function(input_data) {

	if(nrow(input_data) > 0) {
		print("############## NA count for each column ###############")
		# Getting the count of NA values in each column.
		# h2o.colnames - gets the column names of H2O frame.
		naCountDF <- do.call(rbind,lapply(colnames(input_data), function(x) {
		  return(data.frame(x,sum(is.na(input_data[[x]]))))
		}))
		colnames(naCountDF) <- c("attribute_name","NA_count")
		naCountDF <- subset(naCountDF, naCountDF$NA_count != 0)

		# Visualizing the results of NA counts.
		library(ggplot2)
		# Plotting the results using ggplot.
		ggplot(naCountDF,aes(x=attribute_name,y=NA_count,fill=attribute_name)) + geom_bar(stat="identity") + ggtitle("Total Number of NA values for each column") + xlab("Attribute Name") + ylab("NA Count") + theme(axis.text.x = element_text(angle = 90, hjust = 1, vjust = 1)) + geom_text(aes(label=NA_count),vjust = 0.6, angle = 45)
	}
}

# Function used to apply data preprocessing steps like to subset required columns, derive additional features from date and data type conversion.
fn.applyDataPreprocessing <- function(input_data) {

	if(nrow(input_data) > 0) {
		print("############## Data Preprocessing ###############")

		# subsetting only the required columns from source
		crimesubsetDF <- subset(input_data, select = -c(crm_cd_2,crm_cd_3,crm_cd_4,cross_street,premis_cd,weapon_used_cd))
		print(colnames(crimesubsetDF))

		crimePreprocessInput <- crimesubsetDF
		# Converting the crime_reported_date into Date.
		crimePreprocessInput$crime_reported_date <- as.Date(crimePreprocessInput$date_rptd, format = "%Y-%m-%d")
		# Converting the crime_occurred_date into Date.
		crimePreprocessInput$crime_occurred_date <- as.Date(crimePreprocessInput$date_occ, format = "%Y-%m-%d")
		# Ordering the dataset based on crime_occurred_date.
		crimePreprocessInput <- crimePreprocessInput[order(crimePreprocessInput$crime_occurred_date),]

		# Finding the year,month and week from crime_occurred_date using lubridate package.
		library(lubridate)
		# Getting year from date.
		crimePreprocessInput$crime_year <- year(crimePreprocessInput$crime_occurred_date)
		# Getting month from date.
		crimePreprocessInput$crime_month <- month(crimePreprocessInput$crime_occurred_date, label = TRUE)
		# Getting week from date.
		crimePreprocessInput$crime_week <- week(crimePreprocessInput$crime_occurred_date)

		# Finding the time of crime occured from time_occ attribute. Converting the integer into time attribute using chron package.
		library(chron)
		crimePreprocessInput$crime_time <- times(substr(as.POSIXct(sprintf("%04.0f", crimePreprocessInput$time_occ), origin = "1970-01-01","Asia/Calcutta", format='%H%M'),12,19))

		#Finding the time interval using chron package by appending the time interval based on crime_time.
		 crimePreprocessInput$crime_time_interval <- cut(crimePreprocessInput$crime_time, breaks = times(c("00:00:00", "05:00:00", "09:00:00","13:00:00", "17:00:00", "21:00:00", "23:59:00")), labels = c("Late night", "Early morning", "Late morning", "Early afternoon", "Late afternoon", "Evening"), include.lowest=TRUE)

		# Converting the datatypes of certain columns which doesn't suite H2O environment.
		# Making the date columns as character and factor columns
		crimePreprocessInput$crime_reported_date <- as.character(crimePreprocessInput$crime_reported_date)
		crimePreprocessInput$crime_occurred_date <- as.character(crimePreprocessInput$crime_occurred_date)
		crimePreprocessInput$crime_month <- as.factor(as.character(crimePreprocessInput$crime_month))
		crimePreprocessInput$crime_time <- as.character(crimePreprocessInput$crime_time)
				
		return(crimePreprocessInput)
	}
}

# Function initializes H2O cluster and converts Dataframe into H2O frame.
fn.initializeH2OCluster <- function(input_data) {

	print("############## Initializing H2O cluster ###############")
	library(h2o)

	# Initializing H2O cluster with default settings.
	# H2O can be accessed using the port 12345
	h2o.init(port = 12345)

	# Importing the dataframe into the H2O environment
	imputationFrame <- as.h2o(input_data)
	
	return(imputationFrame)

}

# Function used to impute missing values for all columns in the dataframe.
fn.applyDataImputation <- function(input_data) {

	print("############## Data Imputation ###############")

	# In H2o, imputation is done based on default methods mean, median and mode. Methods applied to dataframe is based on data type of each column
	# For eg, factor or categorical columns can be imputed by mode method.
	# Function - h2o.impute.
	# Param - data - h2oframe to be imputed
	#         column - specific to impute, if 0 imputes the whole dataframe.
	#         method - method to impute h2oframe.
	h2o.impute(input_data, column = 0, method = c("mean", "median", "mode"))

	input_data$arrest_status <- as.factor(ifelse(input_data$status_desc == "Invest Cont", "Not Arrested", "Arrested"))
	
	return(input_data)

}

# Function used to apply Autoencoders model to the preprocessed dataframe by splitting it into Train, validatoin and test frames.
# Applies model by tunning the parameters of the model based on different criterias.
fn.applyAutoencoders <- function(input_data) {
	
	if(h2o.nrow(input_data) > 0) {
	
		print("############## Model Training ###############")
		# Subsetting only 2016 year as train set and valid sets
		crime_train_frame <- input_data[input_data$crime_year == 2016,]
		# Model Training
		# h2o.splitFrame spplits the source into train and test test.
		# function - h2o.splitFrame
		# @param data - H2OFrame object to  be splitted as train and valid datasets
		# @param ratios - ratios in which the datas is to be splitted.
		# @param seed - setting seed for reproducability.
		h2oSplitFrame <- h2o.splitFrame(data = crime_train_frame, ratios = c(0.75), seed = 123)
		# Assigning the train, test and validation frames to a variable.
		# Making the training frame.
		training_frame <- h2o.assign(h2oSplitFrame[[1]], key = "training_frame")
		print(h2o.nrow(training_frame))
		# Making the validation frame.
		validation_frame <- h2o.assign(h2oSplitFrame[[2]], key = "validation_frame")
		print(h2o.nrow(validation_frame))
		# Subsetting the test frame for the year 2017.
		crime_test_frame <- input_data[input_data$crime_year == 2017,]
		print(h2o.nrow(crime_test_frame))
		
		print("############## Deep Learning - Autoencoders ###############")

		# The dependent variable of our data.
		response <- "arrest_status"
		# Features we are going to use in our model.
		features <- c("area_id","rpt_dist_no","crm_cd","vict_age","vict_sex","vict_descent","premis_desc","weapon_desc")

		# h2o.deeplearning - Used to apply Deep Learning to our data.
		# @param x - features for our model
		# @param training_frame - dataset to which model need to be applied.
		# @param model_id - string represents our model to save and load.
		# @param seed - for resproducability
		# @param hidden - number of hidden layers.
		# @param epochs - number of iterations our dataset must go through
		# @param activation - a string representing the activation to be used
		# @params stopping_rounds, stopping_metric, export_weights_and_biases - Used for crossvalidation purposes.
		# @param autoencoder - logical, representing whether autoencoders should be applied or not
		system.time(crime_model_auto <- h2o.deeplearning(x = features, training_frame = training_frame, validation_frame = validation_frame, model_id = "crime_model_auto", seed = 123456, hidden = c(10,10), epochs = 10, activation = "Tanh", stopping_rounds = 5, stopping_metric = "AUTO", export_weights_and_biases = TRUE, autoencoder = TRUE))
		
		h2o.mean_per_class_error(crime_model_auto, train = TRUE, valid = TRUE, xval = TRUE)

		library(dplyr)
		library(ggplot2)
		# The dimensionality of our model is reduced to get better results. The features of one of the hidden layers are extracted and the results are plotted to classify the arrest status using deep features functions in H2O package.
		train_features <- h2o.deepfeatures(crime_model_auto, training_frame, layer = 2) %>% as.data.frame() %>% mutate(arrest_status = as.vector(training_frame[, 16])) %>% as.h2o()
		ggplot(as.data.frame(train_features), aes(x = DF.L2.C1, y = DF.L2.C2, color = arrest_status, shape = arrest_status)) + geom_point()
		
		set.seed(123)
		test_dim <- h2o.deepfeatures(crime_model_auto, crime_test_frame, layer = 2)
		crime_pred <- h2o.predict(train_features, test_dim)
		crime_pred$actual_value <- crime_test_frame$arrest_status
		
		# Applying deep learning model to extracted features. with the dependent variable.
		crime_pretrained <- h2o.deeplearning(x = train_features, y = response, training_frame = training_frame, validation_frame = validation_frame,  pretrained_autoencoder  = "crime_model_auto", nfolds = 5, keep_cross_validation_fold_assignment = TRUE, fold_assignment = "Stratified", variable_importances = TRUE, model_id = "crime_pretrained", seed = 123457, hidden = c(10,10), epochs = 5, activation = "Tanh", score_each_iteration = TRUE)
		summary(crime_pretrained)
		
		return(crime_pretrained)
	}
}

# Function used to perfrom Crime Data Analysis using Deep Learning - Autoencoders
# Loads the input data, applies preprocessing, Initializes H2O cluster and saves the model as POJO object.
fn.crimeDataAnalysis <- function() {

	# Loading Crime Data.
	crime_data <- fn.loadCrimeData()
	
	# Finding number of NA values in each column and displaying the results
	fn.displayTotalNAcount(crime_data)
	
	# Applying Preprocessing steps and deriving additional features from date.
	crime_data_preprocessed <- fn.applyDataPreprocessing(crime_data)
	
	# Initializing H2O cluster and converting the dataframe into H2OFrame
	crime_h2o_frame <- fn.initializeH2OCluster(crime_data_preprocessed)
	
	# Applying Data Imputation
	crime_imputed_frame <- fn.applyDataImputation(crime_h2o_frame)
	
	# Applying Autoencoders model by splitting the datasets into train, validation and test frames.
	crime_model <- fn.applyAutoencoders(crime_imputed_frame)
	
	print("############## Downloading POJO ###############")

	# The autoencoder model is downloaded as POJO object using h2o.download_pojo
	# @param model - the H2O model
	# @param get_jar - to download h2o-genmodel.jar along with model
	# @param path - path where model and jar should be downloaded
	h2o.download_pojo(model = crime_model, get_jar = FALSE, path = "path_to_crime_model")
	
	print("process ended successfully....")

}

fn.crimeDataAnalysis()