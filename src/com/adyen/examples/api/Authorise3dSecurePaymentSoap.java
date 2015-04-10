package com.adyen.examples.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;

import com.adyen.services.common.BrowserInfo;
import com.adyen.services.payment.PaymentPortType;
import com.adyen.services.payment.PaymentRequest3D;
import com.adyen.services.payment.PaymentResult;
import com.adyen.services.payment.PaymentService;
import com.adyen.services.payment.ServiceException;

/**
 * Authorise 3D Secure payment (SOAP)
 * 
 * 3D Secure (Verifed by VISA / MasterCard SecureCode) is an additional authentication
 * protocol that involves the shopper being redirected to their card issuer where their
 * identity is authenticated prior to the payment proceeding to an authorisation request.
 * 
 * In order to start processing 3D Secure transactions, the following changes are required:
 * 1. Your Merchant Account needs to be confgured by Adyen to support 3D Secure. If you would
 *    like to have 3D Secure enabled, please submit a request to the Adyen Support Team (support@adyen.com).
 * 2. Your integration should support redirecting the shopper to the card issuer and submitting
 *    a second API call to complete the payment.
 *
 * This example demonstrates the second API call to complete the payment using SOAP.
 * See the API Manual for a full explanation of the steps required to process 3D Secure payments.
 * 
 * Please note: using our API requires a web service user. Set up your Webservice user:
 * Adyen CA >> Settings >> Users >> ws@Company. >> Generate Password >> Submit
 * 
 * @link /2.API/Soap/Authorise3dSecurePayment
 * @author Created by Adyen - Payments Made Easy
 */

@WebServlet(urlPatterns = { "/2.API/Soap/Authorise3dSecurePayment" })
public class Authorise3dSecurePaymentSoap extends HttpServlet {

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/**
		 * SOAP settings
		 * - wsdl: the WSDL url you are using (Test/Live)
		 * - wsUser: your web service user
		 * - wsPassword: your web service user's password
		 */
		String wsdl = "https://pal-test.adyen.com/pal/Payment.wsdl";
		String wsUser = "YourWSUser";
		String wsPassword = "YourWSUserPassword";

		/**
		 * Create SOAP client, using classes in adyen-wsdl-cxf.jar library (generated by wsdl2java tool, Apache CXF).
		 * 
		 * @see WebContent/WEB-INF/lib/adyen-wsdl-cxf.jar
		 */
		PaymentService service = new PaymentService(new URL(wsdl));
		PaymentPortType client = service.getPaymentHttpPort();

		// Set HTTP Authentication
		((BindingProvider) client).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, wsUser);
		((BindingProvider) client).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, wsPassword);

		/**
		 * After the shopper's identity is authenticated by the issuer, they will be returned to your
		 * site by sending an HTTP POST request to the TermUrl containing the MD parameter and a new
		 * parameter called PaRes (see API manual). These will be needed to complete the payment.
		 *
		 * To complete the payment, a PaymentRequest3d should be submitted to the authorise3d action
		 * of the web service. The request should contain the following variables:
		 * 
		 * <pre>
		 * - merchantAccount: This should be the same as the Merchant Account used in the original authorise request.
		 * - browserInfo:     It is safe to use the values from the original authorise request, as they
		                      are unlikely to change during the course of a payment.
		 * - md:              The value of the MD parameter received from the issuer.
		 * - paReponse:       The value of the PaRes parameter received from the issuer.
		 * - shopperIP:       The IP address of the shopper. We recommend that you provide this data, as
		                      it is used in a number of risk checks, for example, the number of payment
		                      attempts and location based checks.
		* </pre>
		 */

		// Create new payment request
		PaymentRequest3D paymentRequest = new PaymentRequest3D();
		paymentRequest.setMerchantAccount("YourMerchantAccount");
		paymentRequest.setMd(request.getParameter("MD"));
		paymentRequest.setPaResponse(request.getParameter("PaRes"));
		paymentRequest.setShopperIP("123.123.123.123");

		// Set browser info
		BrowserInfo browserInfo = new BrowserInfo();
		browserInfo.setUserAgent(request.getHeader("User-Agent"));
		browserInfo.setAcceptHeader(request.getHeader("Accept"));
		paymentRequest.setBrowserInfo(browserInfo);
		
		/**
		 * Send the authorise3d request.
		 */
		PaymentResult paymentResult;
		try {
			paymentResult = client.authorise3D(paymentRequest);
		} catch (ServiceException e) {
			throw new ServletException(e);
		}

		/**
		 * If the payment passes validation a risk analysis will be done and, depending on the outcome, an authorisation
		 * will be attempted. You receive a payment response with the following fields:
		 * 
		 * <pre>
		 * - pspReference    : Adyen's unique reference that is associated with the payment.
		 * - resultCode      : The result of the payment. Possible values: Authorised, Refused, Error or Received.
		 * - authCode        : The authorisation code if the payment was successful. Blank otherwise.
		 * - refusalReason   : Adyen's mapped refusal reason, populated if the payment was refused.
		 * </pre>
		 */
		PrintWriter out = response.getWriter();

		out.println("Payment Result:");
		out.println("- pspReference: " + paymentResult.getPspReference());
		out.println("- resultCode: " + paymentResult.getResultCode());
		out.println("- authCode: " + paymentResult.getAuthCode());
		out.println("- refusalReason: " + paymentResult.getRefusalReason());

	}

}