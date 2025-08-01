<filename>hybris/bin/y-ext/ext-worldpay/worldpayapi/src/com/worldpay/service/marshalling/impl/DefaultPaymentServiceMarshaller.java<fim_prefix>

        package com.worldpay.service.marshalling.impl;

import com.worldpay.exception.WorldpayException;
import com.worldpay.exception.WorldpayModelTransformationException;
import com.worldpay.exception.WorldpayValidationException;
import com.worldpay.internal.model.PaymentService;
import com.worldpay.service.marshalling.PaymentServiceMarshaller;
import com.worldpay.util.WorldpayConstants;
import org.apache.commons.collections.CollectionUtils;
import org.xml.sax.*;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.MessageFormat;

/**
 * {@inheritDoc}
 * Default implementation
 */
public class DefaultPaymentServiceMarshaller implements PaymentServiceMarshaller {

    /**
     * {@inheritDoc}
     */
    @Override
    public PaymentService unmarshal(final InputStream in) throws WorldpayModelTransformationException {
        <fim_suffix>

        try {
            final Object unmarshalledObject = getUnmarshaller().unmarshal(source);
            if (unmarshalledObject instanceof PaymentService) {
                return (PaymentService) unmarshalledObject;
            } else {
                throw new WorldpayModelTransformationException("Message received from Worldpay is not a PaymentService message");
            }
        } catch (final JAXBException e) {
            throw new WorldpayModelTransformationException("Message received from Worldpay is not a PaymentService message", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String marshal(final PaymentService paymentService) throws WorldpayException {
        final StringWriter sw = new StringWriter();
        try {
            validate(paymentService);
            getMarshaller().marshal(paymentService, sw);
        } catch (JAXBException e) {
            throw new WorldpayModelTransformationException("Failed to marshal paymentService generated by the worldpay response mock", e);
        }
        return sw.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String marshalAsFragment(final PaymentService paymentService) throws WorldpayException {
        final StringWriter sw = new StringWriter();
        try {
            validate(paymentService);
            final Marshaller marshaller = getMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.marshal(paymentService, sw);
        } catch (JAXBException e) {
            throw new WorldpayModelTransformationException("Failed to marshal paymentService generated by the worldpay response mock", e);
        }
        return sw.toString();
    }

    private SAXSource getSAXSourceFromInputStream(final InputStream in) throws WorldpayModelTransformationException {
        final SAXParserFactory spf = SAXParserFactory.newInstance();
        XMLReader xmlReader;
        try {
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            xmlReader = spf.newSAXParser().getXMLReader();
        } catch (final SAXNotRecognizedException e) {
            throw new WorldpayModelTransformationException(MessageFormat.format("Tried to set an unrecognized feature ({0}) on the saxParserFactory", XMLConstants.FEATURE_SECURE_PROCESSING), e);
        } catch (final SAXNotSupportedException e) {
            throw new WorldpayModelTransformationException(MessageFormat.format("Tried to set an unsupported feature ({0}) on the saxParserFactory", XMLConstants.FEATURE_SECURE_PROCESSING), e);
        } catch (final ParserConfigurationException e) {
            throw new WorldpayModelTransformationException("Serious configuration error when generating the saxParserFactory", e);
        } catch (final SAXException e) {
            throw new WorldpayModelTransformationException("General SAX error occurred during processing xml", e);
        }

        final InputSource inputSource = new InputSource(in);
        return new SAXSource(xmlReader, inputSource);
    }

    protected Unmarshaller getUnmarshaller() throws JAXBException {
        return WorldpayConstants.JAXB_CONTEXT.createUnmarshaller();
    }

    protected Marshaller getMarshaller() throws JAXBException {
        return WorldpayConstants.JAXB_CONTEXT.createMarshaller();
    }

    private void validate(final PaymentService paymentService) throws WorldpayValidationException {
        if (CollectionUtils.isEmpty(paymentService.getSubmitOrModifyOrInquiryOrReplyOrNotifyOrVerify()))
            throw new WorldpayValidationException("No notification target in Worldpay target");
    }
}

<fim_middle>