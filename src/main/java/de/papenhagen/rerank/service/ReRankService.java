package de.papenhagen.rerank.service;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import de.papenhagen.rerank.controller.NotFoundException;
import de.papenhagen.rerank.dto.RankingDTO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class ReRankService {
    //"cross-encoder/ms-marco-MiniLM-L6-v2
    final Path modelPath = Path.of("model.onnx");


    public ReRankService() {
        if (!Files.exists(modelPath)) {
            throw new IllegalArgumentException("Model file not found: " + modelPath.toAbsolutePath());
        }
    }

    public String rerank(final List<RankingDTO> pairs) {
        try (// Load tokenizer
             final HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(modelPath);
             // load the env for onnx model
             final OrtEnvironment env = OrtEnvironment.getEnvironment();
             final OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
             final OrtSession session = env.createSession(modelPath.toAbsolutePath().toString(), opts)
        ) {
            final StringBuilder sb = new StringBuilder();
            for (final RankingDTO p : pairs) {
                final Encoding encoded = tokenizer.encode(p.question(), p.answer());
                final long[] inputIds = encoded.getIds();
                final long[] attentionMask = encoded.getAttentionMask();

                try (final OnnxTensor idsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), new long[]{1, inputIds.length});
                     final OnnxTensor maskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), new long[]{1, attentionMask.length})) {

                    final Map<String, OnnxTensor> inputs = Map.of(
                            "input_ids", idsTensor,
                            "attention_mask", maskTensor
                    );

                    try (final OrtSession.Result result = session.run(inputs)) {
                        float[][] logits = (float[][]) result.get(0).getValue();
                        // sigmoid
                        float score = (float) (1 / (1 + Math.exp(-logits[0][0])));

                        sb.append(String.format(
                                "Q: %-50s | Score: %.4f%nA: %s%n%n",
                                p.question(), score, p.answer()
                        ));
                    }
                } catch (OrtException e) {
                    throw new NotFoundException(e);
                }
            }

            return sb.toString();

        } catch (IOException | OrtException e) {
            throw new IllegalArgumentException(e);
        }
    }


}
