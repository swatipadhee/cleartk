/*
 * Copyright (c) 2011, Regents of the University of Colorado 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */

package org.cleartk.eval.provider.corpus.xmi;

import java.util.Arrays;
import java.util.List;

import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.cleartk.eval.Evaluation_ImplBase;

/**
 * Copyright (c) 2011, Regents of the University of Colorado <br>
 * All rights reserved.
 * 
 * @author Philip Ogren
 * @deprecated Tested classes have been replaced by {@link Evaluation_ImplBase}
 */
@Deprecated
public class FixedFoldsXmiTestCorpusFactory extends FixedFoldsXmiCorpusFactory {

  public FixedFoldsXmiTestCorpusFactory(TypeSystemDescription typeSystemDescription) {
    super(
        typeSystemDescription,
        "src/test/resources/eval/provider/corpus/xmi-factory-test-data/xmi");
  }

  public static String[] FOLDS = new String[] {
      "src/test/resources/eval/provider/corpus/xmi-factory-test-data/filenames/fold-1.txt",
      "src/test/resources/eval/provider/corpus/xmi-factory-test-data/filenames/fold-2.txt",
      "src/test/resources/eval/provider/corpus/xmi-factory-test-data/filenames/fold-3.txt",
      "src/test/resources/eval/provider/corpus/xmi-factory-test-data/filenames/fold-4.txt" };

  @Override
  public String getTrainFile() {
    return "src/test/resources/eval/provider/corpus/xmi-factory-test-data/filenames/train.txt";
  }

  @Override
  public String getTestFile() {
    return "src/test/resources/eval/provider/corpus/xmi-factory-test-data/filenames/test.txt";
  }

  @Override
  public void setNumberOfFolds(int numberOfFolds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getFoldFiles() {
    return Arrays.asList(FOLDS);
  }

}
