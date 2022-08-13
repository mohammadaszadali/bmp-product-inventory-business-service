package com.dtag.bm.service.product.inventory.service.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

//import com.dtag.bm.order.service.model.ProductOrder;
import com.dtag.bm.service.product.inventory.service.Application;
import com.dtag.bm.service.product.inventory.service.dao.ProductCsvDAO;
import com.dtag.bm.service.product.inventory.service.dao.ProductInventoryDAO;
import com.dtag.bm.service.product.inventory.service.exceptions.ProductInventorycustomValidatorException;
import com.dtag.bm.service.product.inventory.service.model.CsvUtil;
import com.dtag.bm.service.product.inventory.service.model.CustomerAssociatedSimDetails;
import com.dtag.bm.service.product.inventory.service.model.CustomerAssociatedSims;
import com.dtag.bm.service.product.inventory.service.model.CustomerAssociatedSlices;
import com.dtag.bm.service.product.inventory.service.model.DirectSliceData;
import com.dtag.bm.service.product.inventory.service.model.Product;
import com.dtag.bm.service.product.inventory.service.model.ProductCsv;
import com.dtag.bm.service.product.inventory.service.model.ProductRequest;
import com.dtag.bm.service.product.inventory.service.model.UpdateTerminatedStatusRequest;
import com.dtag.bm.service.product.inventory.service.service.ProductServiceImpl;
import com.dtag.bm.service.product.inventory.service.type.ProductInventoryMain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Stopwatch;

import ch.qos.logback.core.net.SocketConnector.ExceptionHandler;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/productInventoryManagement/v1")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductInventoryController {

	private final String className=this.getClass().getSimpleName();
	
	@Autowired
	ProductServiceImpl productservice;

	@Autowired
	ProductInventoryDAO dao;

	@Autowired
	MongoTemplate mongoTemplate;

	@Autowired
	ProductCsvDAO DAO;

	private static final String FILE_TYPE1 = "text/csv";
	private static final String FILE_TYPE2 = "application/vnd.ms-excel";
	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	/**
	 * @param product
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */

	@PostMapping(value = "/product")
	public Product CreateProducts(@RequestBody Product product) throws IOException, ParseException {
		/* UUID generation logic */
		UUID uuid = UUID.randomUUID();
		String Id = uuid.toString();
		product.setId(Id);
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		if (product.getStatus() == null)
			throw new ProductInventorycustomValidatorException("Status Field Can't be null");
		if (product.getProductOfferingRef() == null && product.getProductSpecificationRef() == null)
			throw new ProductInventorycustomValidatorException(
					"ProductOffering and ProductSpecification both can't be null");
		if (product.getProductCharacteristic().isEmpty())
			throw new ProductInventorycustomValidatorException("Product Characteristic  can't be null");

		Resource<Product> resource = new Resource<Product>(product);
		ControllerLinkBuilder links = linkTo(methodOn(this.getClass()).productDetailsbyId(Id));
		resource.add(links.withRel("get-Product-details"));
		product.setHref(links.toString());

		LOGGER.info("Controller : calling post api for Product request  :\n " + product.toString());
		
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		
		Product savedProduct = productservice.createProductInventory(product);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return savedProduct;
	}

	@PostMapping(value = "/updateProductRemarkByProductInstanceID/{productInstanceID}/{remark}")
	public ResponseEntity<?> upDateProductRemarkByProductInstanceID(
			@PathVariable(value = "productInstanceID") String productInstanceID,

			@PathVariable(value = "remark") String remark) throws Exception {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		if (null != remark) {

			LOGGER.info("Controller : productInstanceID \n" + productInstanceID);
			LOGGER.info("Controller : remark \n" + remark);
			
			LOGGER.info(className+ " " +"findById");
			stopwatch = Stopwatch.createStarted();
			Product product = dao.findById(productInstanceID);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +"findById"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			if (null != product) {
				LOGGER.info(className+ " " +methodName);
				stopwatch = Stopwatch.createStarted();
				String imsi  = productservice.upDateProductRemarkByProductInstanceID(product, remark);
				
				stopwatch.stop();
				LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
				
				return new ResponseEntity<>("Product having IMSI : " + imsi  +" updated successfully with remark : "+remark,
						HttpStatus.OK);
			} else {
				return new ResponseEntity<>("Product with product id: " + productInstanceID + " not available",
						HttpStatus.NOT_FOUND);
			}
		} else {
			return new ResponseEntity<>("Product with product id: " + productInstanceID + "having empty remark",
					HttpStatus.OK);
		}

	}

	/**
	 * @param role
	 * @return
	 */
	@GetMapping("/product/byRole")
	public ResponseEntity<Collection<Product>> GetProducts(@RequestParam("role") String role) {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Responded", "ProductInventoryController");
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		ResponseEntity<Collection<Product>> response = ResponseEntity.accepted().headers(headers).body(dao.findByRelatedPartyRole(role));
	    
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return response;
	}

	/**
	 * @param product
	 * @return
	 * @throws ParseException
	 */
	@PutMapping(value = "/product")
	public Product UpdateProdctInventory(@RequestBody Product product) throws ParseException {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		LOGGER.info(className+ " " +methodName);
		Stopwatch stopwatch = Stopwatch.createStarted();
		
		Product prd = productservice.updateProductInventoryStartDate(product);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return prd;
	}

	/**
	 * @return
	 * @throws IOException
	 */

	@SuppressWarnings("unused")
	@GetMapping(value = "/product")
	public List<Product> getAllProductInventory() throws IOException {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		LOGGER.info(className+ " " +"findAll");
		stopwatch = Stopwatch.createStarted();
		
		List<Product> listOfInventory = dao.findAll();
		stopwatch.stop();
		LOGGER.info(className+ " " +"findAll"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		Query query = new Query();
		query.with(new Sort(Sort.Direction.DESC, "startDate"));
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		
		List<Product> prdList = mongoTemplate.find(query, Product.class);
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return prdList;
	}

	/**
	 * @param id
	 * @return
	 */
	@GetMapping("/product/{id}")
	public Product productDetailsbyId(@PathVariable(value = "id") String id) {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		
		Product prd = dao.findById(id);
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		return prd;
	}

	/**
	 * @param filterKey
	 * @param filterValue
	 * @return
	 */
	@GetMapping("/product/{filterKey}/{filterValue}")
	public List<Product> productsDetails(@PathVariable(value = "filterKey") String filterKey,
			@PathVariable(value = "filterValue") String filterValue) {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		
		List<Product> product = productservice.productsDetails(filterKey, filterValue);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		
		return product;
	}

	/**
	 * @param status
	 * @param id
	 * @return
	 * @throws ParseException
	 */
	@PatchMapping("/product/ChangeStatus/{id}")
	public ResponseEntity<?> updateProdcutStatus(@PathVariable("id") String id) throws ParseException {
		Product ProductToBeUpdated = productDetailsbyId(id);
		
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		if (ProductToBeUpdated != null) {
			ProductToBeUpdated.setStatus("InActive");
			
			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
			productservice.updateProductInventory(ProductToBeUpdated);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			return new ResponseEntity<>("Product with product id: " + id + " updated successfully.", HttpStatus.OK);
		} else {
			return new ResponseEntity<>("Product with product id: " + id + " not available", HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping("/product/updateStatus/{id}/{ExternalId}")
	public ResponseEntity<?> updateStatus(@PathVariable("id") String id, @PathVariable("ExternalId") String ExternalId)
			throws ParseException {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		LOGGER.info(className+ " " +"findById");
		stopwatch = Stopwatch.createStarted();
		
		Product ProductToBeUpdated = dao.findById(id);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +"findById"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		if (ProductToBeUpdated != null) {

			ProductToBeUpdated.setStatus("InActive");
			ProductToBeUpdated.getProductOrderRef().forEach(i -> i.setId(ExternalId));
			
			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
			
			productservice.updateProductInventory(ProductToBeUpdated);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			return new ResponseEntity<>("Product with product id: " + id + "  " + "and ORN Number :" + ExternalId
					+ " updated successfully.", HttpStatus.OK);
		} else {
			return new ResponseEntity<>(
					"Product with product id: " + id + "  " + "and ORN Number :" + ExternalId + " not available",
					HttpStatus.NOT_FOUND);
		}
	}

	/**
	 * @param filterKey
	 * @param filterValue
	 * @param customerId
	 * @return
	 */
	@GetMapping("/product/customerId")
	public Collection<Product> CustomerproductsDetails(@RequestParam(required = false) String filterKey,
			@RequestParam(required = false) String filterValue,
			@RequestParam("RelatedPartyCustomerId") String customerId) {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		if ((filterKey != null) && (filterValue != null)) {
			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
			
			Collection<Product> clc = productservice.CustomerProductDetails(filterKey, filterValue, customerId);
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			return clc;
		} else if (customerId != null) {
			LOGGER.info(className+ " " +"findByRelatedPartyId");
			stopwatch = Stopwatch.createStarted();
			
			Collection<Product> clc = dao.findByRelatedPartyId(customerId);
			stopwatch.stop();
			LOGGER.info(className+ " " +"findByRelatedPartyId"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			return clc;
		}
		return null;
	}

	/**
	 * @param token
	 * @param customerId
	 * @return
	 */
	@GetMapping("/product/retrieve/customerId")
	public Collection<Product> retrieveProdDetailsByType(@RequestParam String token,
			@RequestParam(required = false) String customerId) {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
	
		if ((token != null) && (customerId != null)) {
			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
			
			Collection<Product> clc =  productservice.retrieveProdDetailsByType(token, customerId);
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			return clc;
		} else {
			return null;
		}

	}

	/**
	 * @description this method will return the list of products based on the
	 *              refferedType and status
	 * @param type
	 * @param relatedPartyRefId
	 * @param status
	 * @param relatedPartyreferredType
	 * @return List of products
	 */
	@GetMapping("/product/prodDetailsByType")
	public List<Product> productsDetailsByRefferedType(@RequestParam String type,
			@RequestParam String relatedPartyRefId, @RequestParam String status,
			@RequestParam String relatedPartyreferredType) {
		
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		
		List<Product> product = productservice.productsDetailsByRefferedType(type, relatedPartyRefId, status,
				relatedPartyreferredType);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return product;
	}

	@GetMapping("/product/prodDetails")
	public List<Product> prodDetailsByIdandType(@RequestParam String token,
			@RequestParam(required = false) String filterKey, @RequestParam(required = false) String filterValue,
			@RequestParam(required = false) String customerId) {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		
		List<Product> product = productservice.prodDetailsByIdandTypeTest(token, filterKey, filterValue, customerId);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return product;
	}

	@GetMapping("/product/Slice")
	public List<Product> prodSliceDetails(@RequestParam(required = false) String custId) {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		
		List<Product> product = productservice.prodSliceDetails(custId);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return product;
	}

	@GetMapping("/product/prodDetailsWithSlices")
	public List<ProductRequest> prodDetailsByIdandType1(@RequestParam String token,
			@RequestParam(required = false) String customerId, @RequestParam(required = false) String status) {		
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		
		List<ProductRequest> list = productservice.prodDetailsByTokenAndIdOrId(token, customerId, status);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return list;
	}

	/**
	 * @description "This method will search the record with given inputs, if found
	 *              return the present else Not present.
	 * @param prodId
	 * @param customerId
	 * @param offcerId
	 * @return
	 */
	@GetMapping("/product/checkProdDetails")
	public HashMap<String, String> checkProdDetails(@RequestParam List<String> imsi, @RequestParam String customerId,
			@RequestParam String offerId) {
	
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		
		HashMap<String, String> map = productservice.checkProdDetails(imsi, customerId, offerId);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return map;
	}

	@SuppressWarnings({ "rawtypes", "unchecked", "unused" })
	@PostMapping(value = "/productCsv")
	public ResponseEntity<List<ProductCsv>> uploadCsv(@RequestParam("file") MultipartFile file) throws IOException {

		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		ResponseEntity resp = null;
		String contentType = file.getContentType();
		try {
			LOGGER.info(className+ " " +methodName);
			Stopwatch stopwatchAll = Stopwatch.createStarted();

			if (file.isEmpty() || (!(FILE_TYPE1.equals(contentType)) && !(FILE_TYPE2.equals(contentType)))) {
				return new ResponseEntity("please select a valid csv file!", HttpStatus.OK);
			}

			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();

			productservice.uploadCsv(CsvUtil.readcsv1(ProductCsv.class, file.getInputStream()));
			
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			HttpHeaders headers = new HttpHeaders();
			headers.add("Responded", "ProductInventoryController");

			LOGGER.info(className+ " " +"findAll");
			stopwatch = Stopwatch.createStarted();

			List<ProductCsv> listOfInventory = DAO.findAll();
			
			stopwatch.stop();
			LOGGER.info(className+ " " +"findAll"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			Query query = new Query();
			query.with(new Sort(Sort.Direction.DESC, "Priority"));
			
			LOGGER.info(className+ " " +"find");
			stopwatch = Stopwatch.createStarted();

			mongoTemplate.find(query, ProductCsv.class);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +"find"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			stopwatchAll.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatchAll.elapsed(TimeUnit.MILLISECONDS));
			

			// return new ResponseEntity("File Uploaded Successfully", HttpStatus.OK);
			return new ResponseEntity(listOfInventory, HttpStatus.OK);
		} catch (NumberFormatException ex) {

			ProductCsv prd1 = new ProductCsv();
			return new ResponseEntity(
					"Upload Failed Reason : Invalid data type " + ex.getMessage() + " expected Numeric Value.",
					HttpStatus.BAD_REQUEST);

		}

	}

	@GetMapping("/product/checkOfferDetails")
	public HashMap<String, String> checkOfferDetails(@RequestParam String custId,
			@RequestParam List<String> sliceOfferCodes, @RequestParam String productOfferingCode) {
		
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();

		HashMap<String, String> result = productservice.checkOfferDetails(custId, sliceOfferCodes, productOfferingCode);

		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		
		return result;
	}

	/*
	 * @GetMapping("/CsvDetailsOnLoad") public ResponseEntity<List<ProductCsv>>
	 * GetCsvDetails() { List<ProductCsv> csv = productservice.GetCsvDetails();
	 * HttpHeaders headers = new HttpHeaders(); headers.add("Responded",
	 * "ProductInventoryController");
	 * 
	 * return ResponseEntity.accepted().headers(headers).body(csv); }
	 */

	@GetMapping("/CsvDetailsOnLoad")
	public ResponseEntity<List<ProductCsv>> GetCsvDetails(@RequestParam(required = false) String token) {
		
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
		
		List<ProductCsv> csv = productservice.GetCsvDetails(token);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Responded", "ProductInventoryController");

		return ResponseEntity.accepted().headers(headers).body(csv);
	}

	/**
	 * This will fetch ProductInventoryByExternalId
	 * 
	 * @return
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 * 
	 */
	@SuppressWarnings("unused")
	@GetMapping("/getByExternalId/{ExternalId}")
	public ResponseEntity<?> getProductInventoryByExternalId(@PathVariable(value = "ExternalId") String ExternalId)
			throws com.fasterxml.jackson.core.JsonProcessingException {
		ResponseEntity<?> responseEntity = null;
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		try {

			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();

			List<Product> request = productservice.getProductByExternalId(ExternalId);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			LOGGER.info("size :"+request.size());
			
			if (request != null) {
				try {					
					request.get(0).getTerminationDate();
					
					LOGGER.info("Termination date :"+ request.get(0).getTerminationDate());
					
					String output = request.get(0).getTerminationDate().substring(0, 10);
					
					Date date1=new SimpleDateFormat("yyyy-MM-dd").parse(output);
					
					LOGGER.info("parse date :"+ date1);
					
					 String str = String.format("%ts", date1 );
					 LOGGER.info("epoch date :"+ str);
					 request.get(0).setTerminationDate(str);
				}catch(Exception e) {
					e.printStackTrace();
				}			

				responseEntity = new ResponseEntity<>(request, HttpStatus.OK);
			} else {
				responseEntity = new ResponseEntity<>("Product with specified Id not found", HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseEntity;

	}

	/**
	 * @param id
	 * @return
	 */
	@DeleteMapping("/product/product/{id}")
	public ResponseEntity<?> deleteById(@PathVariable(value = "id") String Id)
			throws com.fasterxml.jackson.core.JsonProcessingException {
		ResponseEntity<?> responseEntity = null;
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		
		try {
			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
			
			Product response = productservice.deleteById(Id);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			if (response != null) {
				responseEntity = new ResponseEntity<>(" Successfully deleted  Product ", HttpStatus.OK);

			} else {
				responseEntity = new ResponseEntity<>("Product with Id " + Id + " not  found ", HttpStatus.NOT_FOUND);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseEntity;

	}

	@SuppressWarnings("unchecked")
	@PostMapping("/product/updateStatusOnterminated")
	public String updateStatusOnterminated(@RequestBody UpdateTerminatedStatusRequest updateStatus) throws IOException {
		updateStatus.getIds().forEach(idone -> {
			Query query = new Query();
			query.addCriteria(Criteria.where("_id").is(idone));

			Update update = new Update();
			update.set("Status", updateStatus.getStatus());
			
			final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
			Stopwatch stopwatch = null;
			
			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
			
			mongoTemplate.updateFirst(query, update, Product.class);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			

		});
		return "Product Temindated Successfully";
	}

	@GetMapping("/product/prodDetailsWithslicePerformance")
	public List<Product> prodDetailsByIdandType(@RequestParam String token,
			@RequestParam(required = false) String customerId, @RequestParam String status,
			@RequestParam(required = false) String pageNumber, @RequestParam(required = false) String pageSize,
			@RequestParam(required = false) String filterKey, @RequestParam(required = false) String filterValue) {
			List<Product> prdList = new ArrayList<Product>();
			final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
			Stopwatch stopwatch = null;
			
			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
			
			prdList =  productservice.latestone(token, customerId, status, pageNumber, pageSize, filterKey, filterValue);
	        
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			return prdList;
	}

	@GetMapping("/product/retrieveImsi/customerId")
	public CustomerAssociatedSims retriveImsisByCustId(@RequestParam String customerId) {
		if (customerId != null) {
			LOGGER.info(className+ " " +"retriveImsisByCustId");
			Stopwatch stopwatch = Stopwatch.createStarted();
			
			CustomerAssociatedSims customerAssociatedSims = productservice.retriveImsisByCustId(customerId);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +"retriveImsisByCustId"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			return customerAssociatedSims;
		} else {
			return null;
		}
	}
	
	@GetMapping("/product/retriveAllCustomerSimDetails")
	public List<CustomerAssociatedSimDetails> retriveAllCustomerSimDetails() {
		List<CustomerAssociatedSimDetails> customerAssSimDetailsList = new ArrayList<CustomerAssociatedSimDetails>();
		LOGGER.info(className+ " " +"retriveAllCustomerSimDetails");
		Stopwatch stopwatch = Stopwatch.createStarted();
		
		customerAssSimDetailsList = productservice.retriveAllCustomerSimDetails();
		
		stopwatch.stop();
		LOGGER.info(className+ " " +"retriveAllCustomerSimDetails"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return customerAssSimDetailsList;

	}

	@GetMapping("/product/retrieveSlices/customerId")
	public List<CustomerAssociatedSlices> retrivesliceDetailsByCustId(@RequestParam String customerId) {
		List<CustomerAssociatedSlices> customerAssSimSlicesList = new ArrayList<CustomerAssociatedSlices>();
		
		if (customerId != null) {
			LOGGER.info(className+ " " +"retrivesliceDetailsByCustId");
			Stopwatch stopwatch = Stopwatch.createStarted();
			
			customerAssSimSlicesList = productservice.retrivesliceDetailsByCustId(customerId);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +"retrivesliceDetailsByCustId"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			return customerAssSimSlicesList;
		} else {
			return null;
		}

	}

	/**
	 * This will fetch ProductInventoryByExternalId
	 * 
	 * @return
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 * 
	 */
	@SuppressWarnings("unused")
	@GetMapping("/activeServicesByImsi/{Imsi}")
	public ResponseEntity<?> getactiveServicesByImsi(@PathVariable(value = "Imsi") String Imsi)
			throws com.fasterxml.jackson.core.JsonProcessingException {
		ResponseEntity<?> responseEntity = null;
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		try {

			LOGGER.info(className+ " " +"acticeServiceByImsi");
			stopwatch = Stopwatch.createStarted();
			
			List<Product> request = productservice.acticeServiceByImsi(Imsi);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +"acticeServiceByImsi"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			if (request != null) {

				responseEntity = new ResponseEntity<>(request, HttpStatus.OK);
			} else {
				responseEntity = new ResponseEntity<>("Products with specified imsi not found", HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseEntity;

	}
	
	
	/**
	 * This will fetch ProductInventoryByExternalId
	 * 
	 * @return
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 * 
	 */
	@SuppressWarnings("unused")
	@GetMapping("/getSimMaterialServiceByImsi/{Imsi}")
	public ResponseEntity<?> getSimMaterialServiceByImsi(@PathVariable(value = "Imsi") String Imsi)
			throws com.fasterxml.jackson.core.JsonProcessingException {
		ResponseEntity<?> responseEntity = null;
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		try {

			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
			
			Product request = productservice.getSimMaterialServiceByImsi(Imsi);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			if (request != null) {

				responseEntity = new ResponseEntity<>(request, HttpStatus.OK);
			} else {
				responseEntity = new ResponseEntity<>("Products with specified imsi not found", HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseEntity;

	}
	
	
	/**
	 * This will fetch UDMProfileID by IMSI
	 * 
	 * @return
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 * 
	 */
	@SuppressWarnings("unused")
	@GetMapping("/fetchUDMProfileIDByImsi/{imsi}")
	public ResponseEntity<?> getUDMProfileIDByImsi(@PathVariable(value = "imsi") String imsi)
			throws com.fasterxml.jackson.core.JsonProcessingException {
		ResponseEntity<?> responseEntity = null;
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		try {
			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
			
			String response = productservice.getUDMProfileIDByImsi(imsi.trim());
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			if (response != null) {

				responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
			} else {
				responseEntity = new ResponseEntity<>("UDM profile ID with specified imsi not found", HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseEntity;

	}

	/**
	 * @param status
	 * @param id
	 * @return
	 * @throws ParseException
	 */
	@GetMapping("/product/activateProductSubscription/{ExternalId}")
	public ResponseEntity<?> activateProductSubscription(@PathVariable("ExternalId") String ExternalId)
			throws ParseException {
		// Product ProductToBeUpdated = dao.findById(id);
		
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		LOGGER.info(className+ " " +"getProductByExternalId");
		stopwatch = Stopwatch.createStarted();
			
		List<Product> request = productservice.getProductByExternalId(ExternalId);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +"getProductByExternalId"+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		
		if (!request.isEmpty()) {
			for (Product ProductToBeUpdated : request) {
				ProductToBeUpdated.setStatus("Active");
				ProductToBeUpdated.getProductOrderRef().forEach(i -> i.setId(ExternalId));
				
				LOGGER.info(className+ " " +methodName);
				stopwatch = Stopwatch.createStarted();
					
				productservice.activateProductSubscription(ProductToBeUpdated);
				
				stopwatch.stop();
				LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
				
				return new ResponseEntity<>("ORN Number :" + ExternalId + " status updated successfully.",
						HttpStatus.OK);
				
			}

		} else {
			return new ResponseEntity<>(" ORN Number :" + ExternalId + " not available", HttpStatus.NOT_FOUND);
		}
		return null;

	}
	
	@GetMapping("/product/getRemarkByListIMSI")
	public HashMap<String, String> getRemarkByListIMSI(@RequestParam List<String> imsi) {
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		LOGGER.info(className+ " " +methodName);
		stopwatch = Stopwatch.createStarted();
			
		HashMap<String, String> map = productservice.getRemarkByListIMSI(imsi);
		
		stopwatch.stop();
		LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
		
		return map;
	}
	
	
	/**
	 * This will fetch SST,SD and PLMN by IMSI
	 * 
	 * @return
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 * 
	 */
	@SuppressWarnings("unused")
	@GetMapping("/fetchDirectSliceDataByIMSI/{Imsi}")
	public ResponseEntity<?> fetchDirectSliceDataByIMSI(@PathVariable(value = "Imsi") String Imsi)
			throws com.fasterxml.jackson.core.JsonProcessingException {
		ResponseEntity<?> responseEntity = null;
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		
		try {


			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
				
			List<DirectSliceData> response = productservice.fetchDirectSliceDataByIMSI(Imsi);
			
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			if (response != null && !response.isEmpty()) {

				responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
			} else {
				responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseEntity;

	}
	
	/**
	 * This will fetch  Product Inventory TMF formated data
	 * 
	 * @return
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 * 
	 */
	@SuppressWarnings("unused")
	@GetMapping("/modifiedTMF/v1/{internalId}")
	public ResponseEntity<?> getNewProdInvenResp(@PathVariable(value = "internalId") String idORN) throws com.fasterxml.jackson.core.JsonProcessingException {
		ResponseEntity<?> responseEntity = null;
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		try {
			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
				
			List<ProductInventoryMain> main = productservice.getNewProdInventResp(idORN.trim());
			
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			responseEntity = new ResponseEntity<>(main, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseEntity;

	}
	
	
	/**
	 * This will fetch  ProductInventory by matching order with SST,SD,DNN,IMSI
	 * 
	 * @return
	 * @throws com.fasterxml.jackson.core.JsonProcessingException
	 * 
	 */
	@SuppressWarnings("unused")
	@GetMapping("/productInventoryManagement/v1/getMatchingOrderBySstSdDnnImsi")
	public ResponseEntity<?> getMatchingOrderBySstSdDnnImsi(@RequestParam String sst, @RequestParam String sd, @RequestParam String dnn ,@RequestParam String customerId,@RequestParam String imsi) throws Exception {
		ResponseEntity<?> responseEntity = null;
		final String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
		Stopwatch stopwatch = null;
		try {
			LOGGER.info(className+ " " +methodName);
			stopwatch = Stopwatch.createStarted();
				
			List<Product> product = productservice.getSstSdDnn(sst.trim(),sd.trim(),dnn.trim(),customerId.trim(),imsi.trim());
			
			stopwatch.stop();
			LOGGER.info(className+ " " +methodName+ " " +stopwatch.elapsed(TimeUnit.MILLISECONDS));
			
			responseEntity = new ResponseEntity<>(product, HttpStatus.OK);
		}catch (Exception ex) {
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_ACCEPTABLE);
		}
		
		return responseEntity;

	}
	

}